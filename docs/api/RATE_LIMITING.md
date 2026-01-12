# Rate Limiting

Documentation for rate limiting policies in the Car Rental API.

## Table of Contents

- [Overview](#overview)
- [Current Status](#current-status)
- [Planned Implementation](#planned-implementation)
- [Rate Limit Headers](#rate-limit-headers)
- [Handling 429 Responses](#handling-429-responses)
- [Best Practices](#best-practices)

---

## Overview

Rate limiting protects the API from abuse and ensures fair usage among all clients. It limits the number of requests a client can make within a specified time window.

---

## Current Status

> ⚠️ **Not Yet Implemented**
>
> Rate limiting is currently **not implemented** in the Car Rental API. All endpoints accept unlimited requests. This section documents the planned implementation and provides guidance for when rate limiting is enabled.

### Why Rate Limiting is Planned

1. **DDoS Protection**: Prevent denial-of-service attacks
2. **Fair Usage**: Ensure equal access for all API consumers
3. **Resource Management**: Protect backend services from overload
4. **Cost Control**: Limit external API calls (Stripe, OAuth providers)

---

## Planned Implementation

When implemented, rate limiting will use the following tiers:

### Rate Limit Tiers

| Tier | Endpoints | Limit | Window |
|------|-----------|-------|--------|
| **Public** | `/api/cars/*` (read), `/api/exchange-rates/*` | 100 requests | 1 minute |
| **Authenticated** | `/api/rentals/*`, `/api/pricing/*` | 200 requests | 1 minute |
| **Admin** | `/api/admin/*` | 500 requests | 1 minute |
| **Auth** | `/api/auth/login`, `/api/auth/register` | 10 requests | 1 minute |
| **Webhooks** | `/api/webhooks/*` | Unlimited | - |

### Per-Endpoint Limits (Planned)

| Endpoint | Limit | Window | Reason |
|----------|-------|--------|--------|
| `POST /api/auth/login` | 5 | 1 minute | Brute-force protection |
| `POST /api/auth/register` | 3 | 1 minute | Spam prevention |
| `POST /api/rentals` | 10 | 1 minute | Booking abuse prevention |
| `POST /api/payments/*` | 5 | 1 minute | Payment fraud prevention |

---

## Rate Limit Headers

When rate limiting is enabled, these headers will be included in responses:

### Standard Headers

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1704902400
```

| Header | Description |
|--------|-------------|
| `X-RateLimit-Limit` | Maximum requests allowed in the current window |
| `X-RateLimit-Remaining` | Requests remaining in the current window |
| `X-RateLimit-Reset` | Unix timestamp when the window resets |

### On Rate Limit Exceeded

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1704902400
```

| Header | Description |
|--------|-------------|
| `Retry-After` | Seconds to wait before retrying |

---

## Handling 429 Responses

When rate limited, the API returns:

### Response Format

```json
{
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded. Please retry after 60 seconds.",
  "timestamp": "2026-01-10T14:30:00",
  "status": 429,
  "path": "/api/cars",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### Client Handling Example (JavaScript)

```javascript
async function fetchWithRetry(url, options, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    const response = await fetch(url, options);
    
    if (response.status === 429) {
      const retryAfter = response.headers.get('Retry-After') || 60;
      console.log(`Rate limited. Retrying after ${retryAfter} seconds...`);
      await new Promise(resolve => setTimeout(resolve, retryAfter * 1000));
      continue;
    }
    
    return response;
  }
  throw new Error('Max retries exceeded');
}
```

### Client Handling Example (Java)

```java
public Response fetchWithRetry(String url, int maxRetries) {
    for (int i = 0; i < maxRetries; i++) {
        Response response = httpClient.get(url);
        
        if (response.getStatus() == 429) {
            int retryAfter = response.getHeader("Retry-After", 60);
            Thread.sleep(retryAfter * 1000L);
            continue;
        }
        
        return response;
    }
    throw new RuntimeException("Max retries exceeded");
}
```

---

## Best Practices

### For API Consumers

#### 1. Implement Exponential Backoff

```javascript
async function exponentialBackoff(fn, maxRetries = 5) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      if (error.status !== 429 || i === maxRetries - 1) throw error;
      
      const delay = Math.min(1000 * Math.pow(2, i), 30000); // Max 30 seconds
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }
}
```

#### 2. Cache Responses

```javascript
// Cache car listings for 5 minutes
const cache = new Map();

async function getCars() {
  const cacheKey = 'cars-list';
  const cached = cache.get(cacheKey);
  
  if (cached && Date.now() - cached.timestamp < 300000) {
    return cached.data;
  }
  
  const data = await fetch('/api/cars').then(r => r.json());
  cache.set(cacheKey, { data, timestamp: Date.now() });
  return data;
}
```

#### 3. Use Bulk Endpoints

Instead of:
```javascript
// ❌ Bad: 10 separate requests
for (const id of carIds) {
  await fetch(`/api/cars/${id}`);
}
```

Use:
```javascript
// ✅ Good: Single request with multiple IDs
await fetch(`/api/cars?ids=${carIds.join(',')}`);
```

#### 4. Monitor Rate Limit Headers

```javascript
function checkRateLimitHeaders(response) {
  const remaining = response.headers.get('X-RateLimit-Remaining');
  const reset = response.headers.get('X-RateLimit-Reset');
  
  if (remaining && parseInt(remaining) < 10) {
    console.warn(`Low rate limit: ${remaining} requests remaining`);
    console.warn(`Resets at: ${new Date(parseInt(reset) * 1000)}`);
  }
}
```

#### 5. Implement Request Queuing

For high-throughput applications:
- Queue requests and process them at a controlled rate
- Use libraries like `p-queue` (JavaScript) or `RateLimiter` (Java)
- Respect the `X-RateLimit-Remaining` header

### For API Developers (Future Reference)

When implementing rate limiting, consider:

1. **Bucket4j** or **Resilience4j** for Spring Boot
2. **Redis** for distributed rate limiting across instances
3. **Sliding window** algorithm for smoother rate limiting
4. **Token bucket** algorithm for burst allowance
5. **Different limits** for authenticated vs anonymous users
6. **Graceful degradation** during high load

---

## Related Documentation

- [ERROR_CODES.md](./ERROR_CODES.md) - Complete error code documentation
- [API_CONVENTIONS.md](./API_CONVENTIONS.md) - API standards and conventions
