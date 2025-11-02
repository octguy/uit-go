package uitgo.driverservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import uitgo.driverservice.service.CompensatingTransactionService;
import uitgo.driverservice.service.FailureMetricsService;

/**
 * Configuration for scheduled monitoring and health checks
 * Provides automated monitoring of system health and failure metrics
 */
@Slf4j
@Configuration
@EnableScheduling
public class MonitoringScheduleConfig {

    private final CompensatingTransactionService compensatingTransactionService;
    private final FailureMetricsService failureMetricsService;

    @Value("${driver.monitoring.health-check-interval-minutes:5}")
    private int healthCheckIntervalMinutes;

    @Value("${driver.monitoring.metrics-log-interval-minutes:15}")
    private int metricsLogIntervalMinutes;

    @Autowired
    public MonitoringScheduleConfig(CompensatingTransactionService compensatingTransactionService,
                                  FailureMetricsService failureMetricsService) {
        this.compensatingTransactionService = compensatingTransactionService;
        this.failureMetricsService = failureMetricsService;
    }

    /**
     * Perform periodic health checks and consistency validation
     * Runs every 5 minutes by default (configurable)
     */
    @Scheduled(fixedRateString = "#{${driver.monitoring.health-check-interval-minutes:5} * 60 * 1000}")
    public void performScheduledHealthCheck() {
        try {
            log.debug("Starting scheduled health check...");
            compensatingTransactionService.performConsistencyCheck();
            log.debug("Scheduled health check completed");
        } catch (Exception e) {
            log.error("Scheduled health check failed", e);
        }
    }

    /**
     * Log detailed metrics summary for monitoring
     * Runs every 15 minutes by default (configurable)
     */
    @Scheduled(fixedRateString = "#{${driver.monitoring.metrics-log-interval-minutes:15} * 60 * 1000}")
    public void logMetricsSummary() {
        try {
            log.info("=== SCHEDULED METRICS REPORT ===");
            failureMetricsService.logMetricsSummary();
            failureMetricsService.performHealthCheck();
        } catch (Exception e) {
            log.error("Scheduled metrics logging failed", e);
        }
    }

    /**
     * Daily cleanup and maintenance tasks
     * Runs once per day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void performDailyMaintenance() {
        try {
            log.info("Starting daily maintenance tasks...");
            
            // Log comprehensive daily report
            log.info("=== DAILY FAILURE METRICS REPORT ===");
            failureMetricsService.logMetricsSummary();
            
            // Perform deep consistency check
            compensatingTransactionService.performConsistencyCheck();
            
            log.info("Daily maintenance tasks completed");
        } catch (Exception e) {
            log.error("Daily maintenance tasks failed", e);
        }
    }
}