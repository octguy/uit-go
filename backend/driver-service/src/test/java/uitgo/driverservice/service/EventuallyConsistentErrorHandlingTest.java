package uitgo.driverservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for eventually consistent error handling mechanisms
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
class EventuallyConsistentErrorHandlingTest {

    @Autowired
    private RetryableOperationService retryableOperationService;
    
    @Autowired
    private FailureMetricsService failureMetricsService;

    @BeforeEach
    void setUp() {
        // Clear metrics before each test
        failureMetricsService.clearMetrics();
    }

    @Test
    @DisplayName("Retry mechanism should succeed on first attempt")
    void shouldSucceedOnFirstAttempt() throws ExecutionException, InterruptedException, TimeoutException {
        UUID driverId = UUID.randomUUID();
        AtomicInteger executionCount = new AtomicInteger(0);

        CompletableFuture<Void> result = retryableOperationService.executeWithRetry(
                "test-operation",
                () -> {
                    executionCount.incrementAndGet();
                    // Succeed immediately
                },
                driverId
        );

        result.get(5, TimeUnit.SECONDS);
        assertEquals(1, executionCount.get(), "Operation should execute only once");
    }

    @Test
    @DisplayName("Retry mechanism should retry on failure and eventually succeed")
    void shouldRetryAndEventuallySucceed() throws ExecutionException, InterruptedException, TimeoutException {
        UUID driverId = UUID.randomUUID();
        AtomicInteger executionCount = new AtomicInteger(0);

        CompletableFuture<Void> result = retryableOperationService.executeWithRetry(
                "retry-and-succeed-operation",
                () -> {
                    int count = executionCount.incrementAndGet();
                    if (count < 3) {
                        throw new RuntimeException("Simulated failure on attempt " + count);
                    }
                    // Succeed on third attempt
                },
                driverId
        );

        result.get(15, TimeUnit.SECONDS);
        assertEquals(3, executionCount.get(), "Operation should succeed on third attempt");
    }

    @Test
    @DisplayName("Retry mechanism should exhaust all attempts and fail")
    void shouldExhaustRetriesAndFail() {
        UUID driverId = UUID.randomUUID();
        AtomicInteger executionCount = new AtomicInteger(0);

        CompletableFuture<Void> result = retryableOperationService.executeWithRetry(
                "exhaust-retries-operation",
                () -> {
                    executionCount.incrementAndGet();
                    throw new RuntimeException("Always fails");
                },
                driverId
        );

        assertThrows(ExecutionException.class, () -> result.get(15, TimeUnit.SECONDS));
        assertEquals(3, executionCount.get(), "Operation should retry 3 times before failing");
    }

    @Test
    @DisplayName("Failure metrics should track retry attempts correctly")
    void shouldTrackRetryMetricsCorrectly() throws ExecutionException, InterruptedException, TimeoutException {
        UUID driverId = UUID.randomUUID();
        String operationName = "metrics-test-operation";
        AtomicInteger executionCount = new AtomicInteger(0);

        // Test successful retry
        retryableOperationService.executeWithRetry(
                operationName,
                () -> {
                    int count = executionCount.incrementAndGet();
                    if (count < 2) {
                        throw new RuntimeException("Fail once");
                    }
                    // Succeed on second attempt
                },
                driverId
        ).get(5, TimeUnit.SECONDS);

        FailureMetricsService.OperationMetrics metrics = failureMetricsService.getMetrics(operationName);
        assertNotNull(metrics, "Metrics should be recorded");
        assertEquals(1, metrics.retryAttempts.get(), "Should record 1 retry attempt");
        assertEquals(1, metrics.retrySuccesses.get(), "Should record 1 retry success");
        assertEquals(0, metrics.retryExhaustions.get(), "Should record 0 retry exhaustions");
    }

    @Test
    @DisplayName("Failure metrics should track exhausted retries")
    void shouldTrackExhaustedRetries() {
        UUID driverId = UUID.randomUUID();
        String operationName = "track-exhausted-retries-operation";

        // Test exhausted retries
        CompletableFuture<Void> result = retryableOperationService.executeWithRetry(
                operationName,
                () -> {
                    throw new RuntimeException("Always fails");
                },
                driverId
        );

        assertThrows(ExecutionException.class, () -> result.get(15, TimeUnit.SECONDS));

        FailureMetricsService.OperationMetrics metrics = failureMetricsService.getMetrics(operationName);
        assertNotNull(metrics, "Metrics should be recorded");
        assertEquals(2, metrics.retryAttempts.get(), "Should record 2 retry attempts (attempts 2 and 3)");
        assertEquals(0, metrics.retrySuccesses.get(), "Should record 0 retry successes");
        assertEquals(1, metrics.retryExhaustions.get(), "Should record 1 retry exhaustion");
    }

    @Test
    @DisplayName("Async operations should not block main thread")
    void shouldExecuteAsyncOperationsWithoutBlocking() throws ExecutionException, InterruptedException, TimeoutException {
        UUID driverId = UUID.randomUUID();
        AtomicInteger executionCount = new AtomicInteger(0);

        CompletableFuture<Void> result = retryableOperationService.executeAsync(
                "async-test-operation",
                () -> {
                    executionCount.incrementAndGet();
                    try {
                        Thread.sleep(100); // Simulate some work
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                },
                driverId
        );

        // Operation should complete asynchronously
        result.get(5, TimeUnit.SECONDS);
        assertEquals(1, executionCount.get(), "Async operation should execute once");
    }

    @Test
    @DisplayName("Failure metrics should provide comprehensive summary")
    void shouldProvideComprehensiveMetricsSummary() throws ExecutionException, InterruptedException, TimeoutException {
        UUID driverId = UUID.randomUUID();
        
        // Create different types of operations with various outcomes
        
        // Successful operation
        retryableOperationService.executeWithRetry("success-operation", () -> {
            // Success immediately
        }, driverId).get(5, TimeUnit.SECONDS);
        
        // Operation that succeeds after retry
        AtomicInteger retryCount = new AtomicInteger(0);
        retryableOperationService.executeWithRetry("retry-success-operation", () -> {
            if (retryCount.incrementAndGet() < 2) {
                throw new RuntimeException("Fail once");
            }
        }, driverId).get(5, TimeUnit.SECONDS);
        
        // Failed async operation
        retryableOperationService.executeAsync("async-fail-operation", () -> {
            throw new RuntimeException("Async failure");
        }, driverId).get(5, TimeUnit.SECONDS);
        
        String summary = failureMetricsService.getMetricsSummary();
        assertNotNull(summary, "Metrics summary should be generated");
        assertTrue(summary.contains("FAILURE METRICS SUMMARY"), "Summary should contain header");
        assertTrue(summary.contains("success-operation"), "Summary should contain successful operation");
        assertTrue(summary.contains("retry-success-operation"), "Summary should contain retry operation");
    }
}