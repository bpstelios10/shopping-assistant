# Caching Strategy

## Goal

Reduce unnecessary calls to external services while keeping data reasonably fresh. Caching is applied only where
repeated requests are expected to return the same (or nearly the same) data.

The main principle:

> Cache stable, frequently requested data. Avoid caching highly variable requests with many possible combinations.

---

## Product Categories

### Cache: `product-categories`

**Decision:** Cache

**Reasoning:**

* Categories change rarely.
* The endpoint has no parameters.
* Every user receives the same data.
* There is no benefit in repeatedly calling the external service.

**Strategy:**

* Single cache entry.
* TTL: 1 hour.

---

## Products by Category

### Cache: `products-per-category`

**Decision:** Cache only category-only searches.

**Reasoning:**

* Requests like "give me products in Electronics" are repeated often.
* Searches with many filters have a high number of possible combinations and lower cache efficiency.

Cached:

```text
category=Electronics
query=null
maxPrice=null
```

Not cached:

```text
query=phone
maxPrice=500
category=Electronics
```

---

## Featured / Initial Product List

### Cache: `featured-products`

**Decision:** Cache

**Reasoning:**

* The endpoint always returns a small fixed page of products.
* Multiple users requesting the first page will likely receive the same data.
* Repeating the same database/external service call provides little value.

**Strategy:**

* Single cache entry.
* Short TTL (around a few minutes).

---

## Cache Design Rules

* Do not cache everything by default.
* Prefer caching data with predictable keys and repeated access patterns.
* Keep TTL based on how often data changes.
* Use separate caches when different data types need different expiration policies.
