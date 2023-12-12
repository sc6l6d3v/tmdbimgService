# load env vars
export  $(cat .env | grep -v ^\# | xargs)

REDISKEY=$REDISKEY REDISHOST=$REDISHOST \
    TMDBKEY=$TMDBKEY \
    PORT=8080 \
    BINDHOST=0.0.0.0 \
    CLIENTPOOL=128 \
    SERVERPOOL=128 \
docker run --net rs-net \
           --env REDISKEY --env REDISHOST \
     	   --env TMDBKEY --env PORT --env BINDHOST \
           --env CLIENTPOOL --env SERVERPOOL \
           --restart on-failure \
	   --name tmdbimg \
           -d -p 5052:5050 -p 8082:8080 nanothermite/tmdbimg:latest
