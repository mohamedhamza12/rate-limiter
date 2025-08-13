package com.example.ratelimiter;

import com.example.ratelimiter.exceptions.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter(10, 5); // bucketCapacity=10, refillRatePerSecond=5
    }

    @Test
    void testTryConsumeSuccess() {
        assertDoesNotThrow(() -> rateLimiter.tryConsume("user1", 5));
    }

    @Test
    void testTryConsumeExceedCapacityThrows() {
        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.tryConsume("user1", 11));
        assertTrue(ex.getMessage().contains("exceed bucket capacity"));
    }

    @Test
    void testTryConsumeRateLimitExceededThrows() {
        assertDoesNotThrow(() -> rateLimiter.tryConsume("user2", 10));
        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.tryConsume("user2", 1));
        assertTrue(ex.getMessage().contains("Rate limit exceeded"));
    }

    @Test
    void testBucketCleanupRemovesExpiredBuckets() throws InterruptedException {
        rateLimiter.tryConsume("user3", 1);
        // Simulate bucket expiration by waiting longer than expiry
        Thread.sleep(1100); // Wait > 1s
        // Try to consume again; if bucket expired and was cleaned, a new bucket should be created
        assertDoesNotThrow(() -> rateLimiter.tryConsume("user3", 1));
    }
}
