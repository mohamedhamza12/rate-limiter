package com.example.ratelimiter;

import com.example.ratelimiter.exceptions.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
class RateLimiterIntegrationTest {
    @Autowired
    private RateLimiter rateLimiter;

    @Test
    void testMultipleKeysIndependentlyLimited() {
        assertDoesNotThrow(() -> rateLimiter.tryConsume("userA", 5));
        assertDoesNotThrow(() -> rateLimiter.tryConsume("userB", 5));
        assertDoesNotThrow(() -> rateLimiter.tryConsume("userA", 5));
        assertDoesNotThrow(() -> rateLimiter.tryConsume("userB", 5));
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.tryConsume("userA", 1));
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.tryConsume("userB", 1));
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        int threads = 10;
        String key;
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            CountDownLatch latch = new CountDownLatch(threads);
            key = "concurrentUser";
            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    rateLimiter.tryConsume(key, 1);
                    latch.countDown();
                });
            }
            latch.await(2, TimeUnit.SECONDS);
            executor.shutdown();
        }
        assertThrows(RateLimitExceededException.class, () -> rateLimiter.tryConsume(key, 1));
    }

    @Test
    void testBucketExpirationAndRecreation() throws InterruptedException {
        String key = "expireUser";
        assertDoesNotThrow(() -> rateLimiter.tryConsume(key, 1));
        Thread.sleep(1100); // Wait for bucket to expire
        assertDoesNotThrow(() -> rateLimiter.tryConsume(key, 1)); // Should recreate bucket
    }
}
