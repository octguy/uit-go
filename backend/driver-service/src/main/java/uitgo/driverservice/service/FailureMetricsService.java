package uitgo.driverservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for tracking and monitoring failure metrics
 * Provides visibility into retry patterns and failure rates
 */
@Slf4j
@Service
public class FailureMetricsService {

    private final ConcurrentHashMap<String, OperationMetrics> operationMetrics = new ConcurrentHashMap<>();
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Record a retry attempt for an operation
     */
    public void recordRetryAttempt(String operationName, int attemptNumber) {
        OperationMetrics metrics = getOrCreateMetrics(operationName);
        metrics.retryAttempts.incrementAndGet();
        
        log.info("METRICS: Operation {} retry attempt {} recorded. Total retries: {}", 
                operationName, attemptNumber, metrics.retryAttempts.get());
    }

    /**
     * Record a successful retry for an operation
     */
    public void recordRetrySuccess(String operationName, int finalAttemptNumber) {
        OperationMetrics metrics = getOrCreateMetrics(operationName);
        metrics.retrySuccesses.incrementAndGet();
        metrics.lastRetrySuccessTime = LocalDateTime.now();
        
        log.info("METRICS: Operation {} succeeded after {} attempts. Total retry successes: {}", 
                operationName, finalAttemptNumber, metrics.retrySuccesses.get());
    }

    /**
     * Record when all retries are exhausted for an operation
     */
    public void recordRetryExhaustion(String operationName, int maxAttempts) {
        OperationMetrics metrics = getOrCreateMetrics(operationName);
        metrics.retryExhaustions.incrementAndGet();
        metrics.lastRetryExhaustionTime = LocalDateTime.now();
        
        log.error("METRICS: Operation {} exhausted all {} retry attempts. Total exhaustions: {}", 
                 operationName, maxAttempts, metrics.retryExhaustions.get());
        
        // Alert if retry exhaustion rate is high
        if (metrics.retryExhaustions.get() % 10 == 0) {
            log.error("ALERT: Operation {} has {} retry exhaustions - investigate system health!", 
                     operationName, metrics.retryExhaustions.get());
        }
    }

    /**
     * Record an async operation failure
     */
    public void recordAsyncFailure(String operationName) {
        OperationMetrics metrics = getOrCreateMetrics(operationName);
        metrics.asyncFailures.incrementAndGet();
        metrics.lastAsyncFailureTime = LocalDateTime.now();
        
        log.warn("METRICS: Async operation {} failed. Total async failures: {}", 
                operationName, metrics.asyncFailures.get());
    }

    /**
     * Record a compensating transaction
     */
    public void recordCompensatingTransaction(String operationName, String compensationType) {
        OperationMetrics metrics = getOrCreateMetrics(operationName);
        metrics.compensatingTransactions.incrementAndGet();
        metrics.lastCompensatingTransactionTime = LocalDateTime.now();
        
        log.info("METRICS: Compensating transaction {} executed for operation {}. Total compensations: {}", 
                compensationType, operationName, metrics.compensatingTransactions.get());
    }

    /**
     * Get current metrics for an operation
     */
    public OperationMetrics getMetrics(String operationName) {
        return operationMetrics.get(operationName);
    }

    /**
     * Get metrics summary for all operations
     */
    public String getMetricsSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== FAILURE METRICS SUMMARY ===\n");
        summary.append(String.format("Timestamp: %s\n", LocalDateTime.now().format(timestampFormatter)));
        summary.append("Operation Name | Retry Attempts | Retry Successes | Retry Exhaustions | Async Failures | Compensations\n");
        summary.append("─".repeat(110)).append("\n");
        
        operationMetrics.forEach((operationName, metrics) -> {
            summary.append(String.format("%-20s | %-14d | %-15d | %-17d | %-14d | %-12d\n",
                    operationName,
                    metrics.retryAttempts.get(),
                    metrics.retrySuccesses.get(),
                    metrics.retryExhaustions.get(),
                    metrics.asyncFailures.get(),
                    metrics.compensatingTransactions.get()));
        });
        
        return summary.toString();
    }

    /**
     * Log metrics summary to console (for monitoring)
     */
    public void logMetricsSummary() {
        log.info("\n{}", getMetricsSummary());
    }

    /**
     * Check if any operation has concerning failure rates
     */
    public void performHealthCheck() {
        operationMetrics.forEach((operationName, metrics) -> {
            long totalAttempts = metrics.retryAttempts.get() + metrics.retrySuccesses.get();
            long failures = metrics.retryExhaustions.get() + metrics.asyncFailures.get();
            
            if (totalAttempts > 0) {
                double failureRate = (double) failures / totalAttempts;
                if (failureRate > 0.1) { // 10% failure rate threshold
                    log.warn("HEALTH CHECK: Operation {} has high failure rate: {:.2f}% ({} failures out of {} total operations)", 
                            operationName, failureRate * 100, failures, totalAttempts);
                }
            }
        });
    }

    /**
     * Clear all metrics (useful for testing)
     */
    public void clearMetrics() {
        operationMetrics.clear();
    }

    private OperationMetrics getOrCreateMetrics(String operationName) {
        return operationMetrics.computeIfAbsent(operationName, k -> new OperationMetrics());
    }

    /**
     * Metrics container for tracking operation statistics
     */
    public static class OperationMetrics {
        public final AtomicLong retryAttempts = new AtomicLong(0);
        public final AtomicLong retrySuccesses = new AtomicLong(0);
        public final AtomicLong retryExhaustions = new AtomicLong(0);
        public final AtomicLong asyncFailures = new AtomicLong(0);
        public final AtomicLong compensatingTransactions = new AtomicLong(0);
        
        public volatile LocalDateTime lastRetrySuccessTime;
        public volatile LocalDateTime lastRetryExhaustionTime;
        public volatile LocalDateTime lastAsyncFailureTime;
        public volatile LocalDateTime lastCompensatingTransactionTime;
    }
}