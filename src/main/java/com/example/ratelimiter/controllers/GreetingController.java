package com.example.ratelimiter.controllers;

import com.example.ratelimiter.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class GreetingController {
    private final RateLimiter rateLimiter;

    GreetingController(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/greeting")
    public ResponseEntity<String> sayGreeting(HttpServletRequest request,
                                              @RequestParam(value = "name", defaultValue = "World") String name) {
        rateLimiter.tryConsume(request.getRemoteAddr(), 1);
        return ResponseEntity.ok("Hello " + name);
    }
}
