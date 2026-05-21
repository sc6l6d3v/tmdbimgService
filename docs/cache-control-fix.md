# Cache-Control Fix: Preventing Browser Cache Poisoning

## Problem

On desktop, initial loads and search queries frequently displayed the default NoImage placeholder
instead of real posters. Mobile (incognito) loaded all posters correctly, albeit more slowly.
Container logs showed no `getPosterEffect` errors, and Redis keys for the affected items were
present — confirming the service was healthy. Desktop browsers were never contacting the server
at all for those items.

**Root cause:** A prior bug (gzip decompression failure, fixed in commit `7802144`) caused the
service to return default fallback images. Those responses carried `Cache-Control: public,
max-age=86400`, so desktop browsers cached the default images for 24 hours. After the service
was fixed, desktops continued serving the stale cached defaults — bypassing both the server and
Redis entirely. Mobile incognito has no cache, so it always fetched fresh data and got real posters.

---

## Request Flow and Cache Layers

```
Browser request
    │
    ▼
[1] Browser cache check (client-side, up to 24h TTL)
    ├── HIT  → serve cached bytes immediately — SERVER NEVER CONTACTED
    └── MISS → request goes to server
                    │
                    ▼
             [2] TMDBRoutes → getPosterResult → getPosterEffect
                    │
                    ▼
             [3] getMeta(imdbId)
                    │
                    ├── Redis check #1: exists("tmdb:{imdbId}")     [15min TTL]
                    │       ├── HIT  → getMetaFromRedis (cached JSON)
                    │       └── MISS → TMDB Find API → checkSetMeta → setRedisKey
                    │
                    ▼ meta.poster_path
             [4] getPosterImg(posterPath, size)
                    │
                    ├── Redis check #2: exists("tmdbimg:{size}:{posterPath}")  [15min TTL]
                    │       ├── HIT  → getImgFromRedis (cached base64 bytes)
                    │       └── MISS → TMDB Image API → setRedisKey (base64 encoded)
                    │
                    ▼ Option[Array[Byte]]
             [5] None or path missing → defImgMap(size)  ← default fallback
                    │                        │
                    │                   isReal = false
                    │
                    ▼ (bytes, isReal)
             [6] Route applies Cache-Control conditionally  ← FIX POINT
                    ├── isReal = true  → Cache-Control: public, max-age=86400
                    └── isReal = false → Cache-Control: no-store
                    │
                    ▼
             Browser stores response only if isReal
```

### Cache layer comparison

| Cache    | Location   | TTL      | Populated by                  |
|----------|------------|----------|-------------------------------|
| Browser  | Client     | 24 hours | Any HTTP response with Cache-Control |
| Redis    | Server     | 15 min   | Successful TMDB API calls     |

The critical asymmetry: Redis can be updated with fresh data at any time, but a browser holding
a cached response will ignore the server entirely until the TTL expires. A poisoned browser cache
entry (default image cached with `max-age=86400`) dominates for far longer than any Redis entry
is even retained.

---

## Changes Made

### `domains/TMDBImg.scala`

**`getPosterEffect`** — return type changed from `F[Option[Array[Byte]]]` to `F[(Array[Byte], Boolean)]`.
The `Boolean` is `true` only when a real image was retrieved (from Redis or TMDB API), `false` for
any default fallback path. The `L.warn("Using default...")` log remains in both fallback branches.

**`getPosterResult`** — new method added to the `TMDBImg[F]` trait. Delegates to `getPosterEffect`
with error handling, surfacing the `(bytes, isReal)` tuple to the route layer.

**`getPoster`** — updated to destructure the tuple from `getPosterEffect`; discards the flag with `_`
since streaming does not need it. Behavior is otherwise unchanged.

### `routes/TMDBRoutes.scala`

The `meta` route was restructured from a `Stream`-based `Ok(...)` with an unconditional `.map(_.putHeaders(...))`
into a for-comprehension that calls `getPosterResult` and branches on `isReal`:

```scala
case _ @GET -> "meta" /: imdbKey =>
  for {
    pathParts       <- Sync[F].delay(imdbKey.segments.toList)
    _               <- Sync[F].delay(L.info(s""""meta request" key=$imdbKey size=${pathParts.last.encoded}"""))
    (bytes, isReal) <- C.getPosterResult(pathParts.head.encoded, if (pathParts.size == 2) pathParts.tail.head.encoded else "S")
    resp            <- Ok(bytes)
  } yield
    if (isReal)
      resp.putHeaders(`Cache-Control`(CacheDirective.`public`, CacheDirective.`max-age`(86400 seconds)))
    else
      resp.putHeaders(`Cache-Control`(CacheDirective.`no-store`))
```

`Cache-Control: no-store` instructs the browser not to write the response to cache at all, so a
subsequent request for the same URL will always hit the server. `Cache-Control: public, max-age=86400`
is preserved for real images, keeping the 24-hour browser cache behaviour for valid poster data.

---

## Recovery for Existing Poisoned Caches

Desktop browsers that cached default images before this fix will continue serving them until their
24-hour TTL expires. A hard refresh (`Ctrl+Shift+R`) or clearing the cache for the affected image
URLs will force an immediate re-fetch.
