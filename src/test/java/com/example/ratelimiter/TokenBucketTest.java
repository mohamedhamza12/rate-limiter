package com.example.ratelimiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketTest {
    private TokenBucket bucket;

    @BeforeEach
    void setUp() {
        bucket = new TokenBucket(10, 5); // capacity 10, refill 5 tokens/sec
    }

    @Test
    void testTryConsumeSuccess() {
        assertTrue(bucket.tryConsume(5));
    }

    @Test
    void testTryConsumeFailWhenInsufficientTokens() {
        assertTrue(bucket.tryConsume(10));
        assertFalse(bucket.tryConsume(1));
    }

    @Test
    void testRefillTokens() throws InterruptedException {
        assertTrue(bucket.tryConsume(10));
        Thread.sleep(300); // wait for some tokens to refill
        boolean consumed = bucket.tryConsume(1);
        // Should succeed after some time
        assertTrue(consumed);
    }

    @Test
    void testBucketExpiration() throws InterruptedException {
        long expiryNanos = 1_000_000; // 1 ms
        Thread.sleep(2); // sleep > 1ms
        long now = System.nanoTime();
        assertTrue(bucket.hasBucketExpired(now, expiryNanos));
    }
}

