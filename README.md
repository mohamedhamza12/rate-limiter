# Rate Limiter

A simple rate limiter implementation in Java using the Token Bucket algorithm.

## How it Works

This rate limiter uses the **Token Bucket** algorithm. Each unique client (identified by their IP address) is assigned a "bucket" of tokens.

- **Tokens**: Each request from a client consumes one or more tokens from their bucket.
- **Capacity**: Each bucket has a maximum capacity. If the bucket is full, new tokens are discarded.
- **Refill Rate**: Tokens are added to the bucket at a fixed rate until the capacity is reached.
- **Cleanup**: To conserve memory, a background process runs periodically to remove buckets that have been inactive for a configurable amount of time.

This approach allows for bursts of requests while enforcing a long-term average request rate.

## Configuration

The following can be set via `application.properties` or environment variables:

| Property                               | Description                                  | Default    |
|----------------------------------------| -------------------------------------------- |------------|
| `rateLimiter.capacity`                 | Max number of tokens per bucket              | `5`        |
| `rateLimiter.refillRatePerSecond`      | Tokens added per second                      | `1`        |
| `rateLimiter.tokenBucketExpirySeconds` | Time before an inactive bucket is cleaned up | `60` (1 min) |


## Running the application

You can run the application using the following command:

```bash
./gradlew bootRun
```

## Usage Example

With the application running, the rate limiter can be tested through the `/greeting` endpoint.

The configuration allows for a certain number of requests before rate limiting kicks in (defined by `rateLimiter.capacity`). After the capacity is reached, there will be a cooldown period for the bucket to be refilled (defined by `rateLimiter.refillRatePerSecond`) before any further requests are allowed.

Assuming the default `rateLimiter.capacity` of 5:

```bash
# Make 5 successful requests
for i in {1..5}; do curl -i localhost:8080/greeting; done
```

A successful response will look like this:
```
HTTP/1.1 200 OK
Content-Type: text/plain;charset=UTF-8
Content-Length: 12

Hello World
```

```bash
# The next request will be rate limited
curl -i localhost:8080/greeting
```

A rate-limited response will look like this:
```
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
Content-Length: 81

{"message":"Rate limit exceeded for key: <client-ip>"}
```

## Testing

You can run the tests using the following command:

```bash
./gradlew test
```
