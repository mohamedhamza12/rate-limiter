package com.example.ratelimiter.exceptions;

public class RateLimitExceededException  extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
