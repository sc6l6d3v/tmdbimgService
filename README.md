# Obtain TMDB image data given IP

Working Scala REST backend using TMDB API to obtain data for IP address with redis caching.


### Docker

To run in production mode inside a Docker container we first have to build the image. E.g.

```
docker build --build-arg rediskey=$REDISKEY \
             --build-arg redishost=$REDISHOST \
             --build-arg geoipkey=$TMDBKEY \
             --build-arg port=$PORT \
             --build-arg bindhost=$BINDHOST \
             --build-arg clientpool=$CLIENTPOOL \
             --build-arg serverpool=$SERVERPOOL \
             -t tmdbimg:rest .
```

The aforementioned command will build the image and tag it with the latest commit hash.

To run said image:

```
docker run --env MONGOURI --env MONGORO --env REDISKEY --env REDISHOST \
     	   --env TMDBKEY --env PORT --env BINDHOST \
           --env CLIENTPOOL --env SERVERPOOL \
           --restart on-failure \
           --add-host HOST1:IP1 \
           -d -p 8080:8080 tmdbimg:rest
```

To attach to said image via shell:

```
docker exec -it <imagehash> /bin/bash
```
