package com.example.driver_service.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Aspect to count Redis read and write operations
 * Helps measure read-to-write ratio for replica optimization decisions
 */
@Aspect
@Component
@Slf4j
public class RedisOperationCounter {

    private final AtomicLong readCount = new AtomicLong(0);
    private final AtomicLong writeCount = new AtomicLong(0);

    // Intercept all RedisTemplate read operations
    @Around("execution(* org.springframework.data.redis.core.RedisTemplate.opsForValue().get(..)) || " +
            "execution(* org.springframework.data.redis.core.RedisTemplate.opsForValue().multiGet(..)) || " +
            "execution(* org.springframework.data.redis.core.RedisTemplate.opsForGeo().radius(..)) || " +
            "execution(* org.springframework.data.redis.core.RedisTemplate.opsForHash().get(..)) || " +
            "execution(* org.springframework.data.redis.core.RedisTemplate.keys(..))")
    public Object countReadOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        long count = readCount.incrementAndGet();
        log.debug("Redis READ #{}: {}", count, joinPoint.getSignature().toShortString());
        return joinPoint.proceed();
    }

    // Intercept all RedisTemplate write operations
    @Around("execution(* org.springframework.data.redis.core.RedisTemplate.opsForValue().set(..)) || " +
            "execution(* org.springframework.data.redis.core.RedisTemplate.opsForGeo().add(..)) || " +
            "execution(* org.springframework.data.redis.core.RedisTemplate.opsForHash().put(..)) || " +
            "execution(* org.springframework.data.redis.core.RedisTemplate.delete(..))")
    public Object countWriteOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        long count = writeCount.incrementAndGet();
        log.debug("Redis WRITE #{}: {}", count, joinPoint.getSignature().toShortString());
        return joinPoint.proceed();
    }

    public long getReadCount() {
        return readCount.get();
    }

    public long getWriteCount() {
        return writeCount.get();
    }

    public void reset() {
        long reads = readCount.getAndSet(0);
        long writes = writeCount.getAndSet(0);
        log.info("Reset counters - Previous: {} reads, {} writes", reads, writes);
    }

    public void printStats() {
        long reads = readCount.get();
        long writes = writeCount.get();
        double ratio = writes > 0 ? (double) reads / writes : 0;
        
        log.info("Redis Operation Stats:");
        log.info("   Total Reads:  {}", reads);
        log.info("   Total Writes: {}", writes);
        log.info("   Read/Write Ratio: {:.2f}:1", ratio);
        
        if (ratio > 10) {
            log.info("   High read ratio - Read replicas recommended!");
        } else if (ratio > 5) {
            log.info("   Moderate read ratio - Consider read replicas at scale");
        } else {
            log.info("   Low read ratio - Read replicas may not be necessary");
        }
    }
}
