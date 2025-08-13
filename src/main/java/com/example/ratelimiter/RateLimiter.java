package com.example.ratelimiter;

import com.example.ratelimiter.exceptions.RateLimitExceededException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);

    private final int bucketCapacity;
    private final int refillRatePerSecond;
    private final Map<String, TokenBucket> buckets;
    @Value("${rateLimiter.tokenBucketExpirySeconds:60}")
    private int tokenBucketExpirySeconds;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public RateLimiter(@Value("${rateLimiter.capacity:5}") int bucketCapacity,
                       @Value("${rateLimiter.refillRatePerSecond:1}") int refillRatePerSecond) {
        this.bucketCapacity = bucketCapacity;
        this.refillRatePerSecond = refillRatePerSecond;
        buckets = new ConcurrentHashMap<>();
    }

    public void tryConsume(String key, int tokens) throws RateLimitExceededException {
        if (tokens > bucketCapacity) {
            logger.warn("Requested tokens exceed bucket capacity");
            throw new RateLimitExceededException("Requested tokens exceed bucket capacity");
        }

        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(bucketCapacity, refillRatePerSecond));
        boolean hasConsumed = bucket.tryConsume(tokens);

        if (!hasConsumed) {
            throw new RateLimitExceededException("Rate limit exceeded for key: " + key);
        }
    }

    @PostConstruct
    private void startBucketCleanupTask() {
        logger.info("Starting scheduled bucket cleanup task...");
        executor.scheduleAtFixedRate(() -> {
            long now = System.nanoTime();
            long expiryNanos = TimeUnit.SECONDS.toNanos(tokenBucketExpirySeconds);

            buckets.entrySet().removeIf(entry -> entry.getValue().hasBucketExpired(now, expiryNanos));
        }, 0, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    private void shutdown() {
        logger.info("Shutting down scheduled bucket cleanup task...");
        executor.shutdown();
    }
}
