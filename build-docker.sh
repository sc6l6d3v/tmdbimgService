#!/bin/bash
# load env vars
export  $(cat .env | grep -v ^\# | xargs)

    PORT=8080 \
    BINDHOST=0.0.0.0 \
    CLIENTPOOL=128 \
    SERVERPOOL=128 \
docker build --build-arg rediskey=$REDISKEY \
             --build-arg redishost=$REDISHOST \
             --build-arg tmdbkey=$TMDBKEY \
             --build-arg port=$PORT \
             --build-arg bindhost=$BINDHOST \
             --build-arg clientpool=$CLIENTPOOL \
             --build-arg serverpool=$SERVERPOOL \
             --build-arg OPT_PKGS="telnet bash" \
             -t tmdbimg:rest .
