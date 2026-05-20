# TMDB API Double-Gzip CDN Issue (2026-05-20)

## Symptom

`getPosterEffect` fails with `ZipException: Content failed CRC validation` inside
`fs2.compression.CompressionCompanionPlatform.validateTrailer`. The sttp layer wraps
this as `SttpClientException$ReadException`.

Prior to the `handleError` fix, this propagated through the `getPoster` stream and
caused the tmdbimg server to close the HTTP connection mid-transfer, resulting in
`ReachedEndOfStream` on the ratingbunny (upstream) client.

## Root Cause

TMDB's CloudFront CDN had "Compress Objects Automatically" enabled on a misconfigured
edge node (LIS50-P2). The origin was already sending gzip-compressed JSON; CloudFront
added a second gzip layer but only advertised a single `Content-Encoding: gzip` header.
sttp's `HttpClientFs2Backend.standardEncoding` decompressed one layer; fs2's gunzip
then failed CRC validation on the inner gzip stream being treated as a trailer.

Java's `HttpClient` in HTTP/2 mode automatically injects `Accept-Encoding: gzip` on
all requests, which triggered the compressed path. curl (without `--compressed`) did
not send `Accept-Encoding`, so it received uncompressed JSON and worked fine.

TMDB thread: https://www.themoviedb.org/talk/6a0cf9a5dd8f54b8836a3750

## Fixes Applied

**`Main.scala`** — force HTTP/1.1 on the sttp backend so Java's `HttpClient` does not
auto-inject `Accept-Encoding: gzip`:

```scala
dispatcher <- Dispatcher.parallel[IO]
sttpRes    <- Resource.make(
                IO.delay(HttpClientFs2Backend.usingClient[IO](
                  HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                  dispatcher
                ))
              )(_.close())
```

**`TMDBImg.scala`** — belt-and-suspenders: explicitly request uncompressed responses:

```scala
basicRequest.header("Accept-Encoding", "identity").get(tmdbUri)
quickRequest.header("Accept-Encoding", "identity").get(tmdbPosterUri)
```

**`TMDBImg.scala`** — `handleError` in `getPoster` so a TMDB fetch failure serves the
default image instead of closing the connection mid-stream:

```scala
Stream.eval(
  getPosterEffect(imdbId, size).handleError { e =>
    L.error(s"getPosterEffect failed for $imdbId/$size: ${e.getMessage}", e)
    Some(defImgMap(size))
  }
)
```

**`Dockerfile`** — fixed env var name mismatch (`GEOIPKEY` → `TMDBKEY`) so the TMDB
API key is correctly injected from the build arg.

## Behavior While CDN Cache Is Stale

The `ZipException` error log will appear for each request that hits a corrupted CDN
cache entry. The service returns `Default200S.png` or `Default500B.png` as a fallback.
Upstream callers receive a valid chunked response; `ReachedEndOfStream` does not occur.
Cache entries expire naturally; no action required once TMDB resolves the CDN issue.
