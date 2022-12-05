# load env vars
export  $(cat .env | grep -v ^\# | xargs)

REDISKEY=$REDISKEY REDISHOST=192.168.4.47 \
    TMDBKEY=$TMDBKEY \
    PORT=8080 \
    BINDHOST=0.0.0.0 \
    CLIENTPOOL=128 \
    SERVERPOOL=128 \
docker run --env REDISKEY --env REDISHOST \
     	   --env TMDBKEY --env PORT --env BINDHOST \
           --env CLIENTPOOL --env SERVERPOOL \
           --restart on-failure \
           -d -p 8082:8080 tmdbimg:rest

