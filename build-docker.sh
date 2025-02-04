#!/bin/bash
# load env vars
export  $(cat .env | grep -v ^\# | xargs)

docker build --build-arg rediskey=$REDISKEY \
             --build-arg redishost=$REDISHOST \
             --build-arg tmdbkey=$TMDBKEY \
             --build-arg port=$PORT \
             --build-arg bindhost=$BINDHOST \
             --build-arg clientpool=$CLIENTPOOL \
             --build-arg serverpool=$SERVERPOOL \
             --build-arg OPT_PKGS="telnet bash" \
             -t $HUBUSER/tmdbimg:`date +"%Y%m%d%H%M"` \
             -t $HUBUSER/tmdbimg:latest .
