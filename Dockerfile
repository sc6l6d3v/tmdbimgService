#
# Scala and sbt Dockerfile
#
# original file from
# https://github.com/hseeberger/scala-sbt
#

# Pull base image
FROM bellsoft/liberica-openjdk-alpine:21.0.6

# Env variables
ENV SCALA_VERSION=2.13.16
# ENV SBT_VERSION   1.3.10
ENV APP_NAME=tmdbimgService
ENV APP_VERSION=0.1-SNAPSHOT

ARG rediskey=key
ARG redishost=host
ARG tmdbkey=key
ARG port=8080
ARG bindhost=0.0.0.0
ARG clientpool=128
ARG serverpool=128

ENV REDISKEY=$rediskey
ENV REDISHOST=$redishost
ENV GEOIPKEY=$tmdbkey
ENV PORT=$port
ENV BINDHOST=$bindhost
ENV CLIENTPOOL=$clientpool
ENV SERVERPOOL=$serverpool

RUN \
   apk add --no-cache curl bash busybox-extras

# ENV variables for App

# Define working directory
WORKDIR /root
ENV PROJECT_HOME=/usr/src

COPY [".env", "/tmp/build/"]
COPY ["build.sbt", "/tmp/build/"]
COPY ["project/plugins.sbt", "project/build.properties", "/tmp/build/project/"]
#RUN cd /tmp/build && \
# sbt update && \
# sbt compile && \
# sbt assembly

RUN mkdir -p $PROJECT_HOME/data

WORKDIR $PROJECT_HOME/data

# We are running http4s on this port so expose it
EXPOSE 8080
EXPOSE 5050

COPY target/scala-2.13/${APP_NAME}-assembly-$APP_VERSION.jar $PROJECT_HOME/data/$APP_NAME.jar

# This will run at start, it points to the .sh file in the bin directory to start the play app
ENTRYPOINT [ "java", "-Djava.net.preferIPv4Stack=true", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5050",  "-jar", "$PROJECT_HOME/data/$APP_NAME.jar" ]
# Add this arg to the script if you want to enable remote debugging: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
