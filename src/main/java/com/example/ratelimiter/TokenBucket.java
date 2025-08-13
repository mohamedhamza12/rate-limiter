package com.example.ratelimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class TokenBucket {
    private static final Logger logger = LoggerFactory.getLogger(TokenBucket.class);

    private final AtomicReference<Double> availableTokens;
    private final AtomicLong lastRefillAt;
    private final AtomicLong lastUsedAt;
    private final int refillRatePerSecond;
    private final int capacity;


    public TokenBucket(int capacity,  int refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.availableTokens = new AtomicReference<>((double) capacity);
        this.lastRefillAt = new AtomicLong(System.nanoTime());
        this.lastUsedAt = new AtomicLong(System.nanoTime());
    }

    public boolean hasBucketExpired(long now, long expiryNanos) {
        return now - lastUsedAt.get() > expiryNanos;
    }

    public boolean tryConsume(int tokens) {
        lastUsedAt.set(System.nanoTime());
        tryRefill();

        AtomicBoolean consumed = new AtomicBoolean(false);
        double remaining = availableTokens.updateAndGet(v -> {
            if (v >= tokens) {
                consumed.set(true);
                return v - tokens;
            }

            return v;
        });

        if (consumed.get()) {
            logger.debug("Consumed {} tokens. Remaining: {}", tokens, remaining);
            return true;
        }

        logger.warn("Rate limit reached for tokens. Requested {}/{} tokens", tokens, remaining);
        return false;
    }

    private void tryRefill() {
        long now = System.nanoTime();

        while (true) {
            Double previousTokens = availableTokens.get();
            long previousLastRefill = lastRefillAt.get();
            double timeSinceLastRefillInSec = (now - previousLastRefill) / 1_000_000_000.0;
            double tokensToRefill = timeSinceLastRefillInSec * refillRatePerSecond;

            if (tokensToRefill <= 0) {
                return;
            }

            double newTokens = Math.min(capacity, previousTokens + tokensToRefill);
            double tokensRefilled = newTokens - previousTokens;
            long timeUsedInNano = (long) (tokensRefilled / refillRatePerSecond * 1_000_000_000L);
            long newLastRefill = previousLastRefill + timeUsedInNano;

            if (availableTokens.compareAndSet(previousTokens, newTokens)) {
                lastRefillAt.set(newLastRefill);
                break;
            }
        }
    }
}
