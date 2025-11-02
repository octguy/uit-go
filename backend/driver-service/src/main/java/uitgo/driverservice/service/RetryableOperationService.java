package uitgo.driverservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Service for handling retryable operations with exponential backoff
 * Implements eventually consistent pattern for better availability
 */
@Slf4j
@Service
public class RetryableOperationService {

    private final ScheduledExecutorService retryExecutor;
    private final FailureMetricsService failureMetricsService;

    @Value("${driver.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${driver.retry.initial-delay-ms:1000}")
    private long initialDelayMs;

    @Value("${driver.retry.max-delay-ms:30000}")
    private long maxDelayMs;

    @Value("${driver.retry.backoff-multiplier:2.0}")
    private double backoffMultiplier;

    public RetryableOperationService(FailureMetricsService failureMetricsService) {
        this.failureMetricsService = failureMetricsService;
        this.retryExecutor = Executors.newScheduledThreadPool(5, r -> {
            Thread t = new Thread(r, "retry-operation-thread");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Executes an operation with retry logic and exponential backoff
     *
     * @param operationName Name of the operation for logging and metrics
     * @param operation The operation to execute
     * @param entityId ID of the entity being processed (for tracking)
     * @return CompletableFuture that completes when operation succeeds or all retries are exhausted
     */
    public CompletableFuture<Void> executeWithRetry(String operationName, Runnable operation, UUID entityId) {
        return executeWithRetry(operationName, () -> {
            operation.run();
            return null;
        }, entityId);
    }

    /**
     * Executes an operation with retry logic and exponential backoff
     *
     * @param operationName Name of the operation for logging and metrics
     * @param operation The operation to execute
     * @param entityId ID of the entity being processed (for tracking)
     * @param <T> Return type of the operation
     * @return CompletableFuture that completes when operation succeeds or all retries are exhausted
     */
    public <T> CompletableFuture<T> executeWithRetry(String operationName, Supplier<T> operation, UUID entityId) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executeWithRetryInternal(operationName, operation, entityId, 1, initialDelayMs, future);
        return future;
    }

    private <T> void executeWithRetryInternal(String operationName, Supplier<T> operation, UUID entityId, 
                                              int attemptNumber, long delayMs, CompletableFuture<T> future) {
        
        Runnable retryTask = () -> {
            try {
                log.debug("Executing {} for entity: {} (attempt {}/{})", 
                         operationName, entityId, attemptNumber, maxRetryAttempts);
                
                T result = operation.get();
                future.complete(result);
                
                // Record successful retry if this wasn't the first attempt
                if (attemptNumber > 1) {
                    failureMetricsService.recordRetrySuccess(operationName, attemptNumber);
                    log.info("Operation {} succeeded for entity: {} on attempt {}", 
                            operationName, entityId, attemptNumber);
                }
                
            } catch (Exception e) {
                if (attemptNumber >= maxRetryAttempts) {
                    // All retries exhausted
                    failureMetricsService.recordRetryExhaustion(operationName, maxRetryAttempts);
                    log.error("Operation {} failed permanently for entity: {} after {} attempts", 
                             operationName, entityId, maxRetryAttempts, e);
                    future.completeExceptionally(e);
                } else {
                    // Schedule next retry with exponential backoff
                    long nextDelay = Math.min((long) (delayMs * backoffMultiplier), maxDelayMs);
                    failureMetricsService.recordRetryAttempt(operationName, attemptNumber);
                    
                    log.warn("Operation {} failed for entity: {} (attempt {}/{}), retrying in {}ms: {}", 
                            operationName, entityId, attemptNumber, maxRetryAttempts, nextDelay, e.getMessage());
                    
                    retryExecutor.schedule(() -> executeWithRetryInternal(operationName, operation, entityId, 
                                                                         attemptNumber + 1, nextDelay, future), 
                                         nextDelay, TimeUnit.MILLISECONDS);
                }
            }
        };

        if (attemptNumber == 1) {
            // Execute immediately for first attempt
            retryTask.run();
        } else {
            // Schedule delayed execution for retries
            retryExecutor.schedule(retryTask, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Execute operation asynchronously without blocking the main thread
     */
    public CompletableFuture<Void> executeAsync(String operationName, Runnable operation, UUID entityId) {
        return CompletableFuture.runAsync(() -> {
            try {
                operation.run();
                log.debug("Async operation {} completed for entity: {}", operationName, entityId);
            } catch (Exception e) {
                failureMetricsService.recordAsyncFailure(operationName);
                log.error("Async operation {} failed for entity: {}", operationName, entityId, e);
            }
        }, retryExecutor);
    }

    public void shutdown() {
        retryExecutor.shutdown();
        try {
            if (!retryExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                retryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            retryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}