FROM ubuntu:23.04

ENV VERTICLE_FILE vertx-proxy-fat.jar
ENV VERTICLE_HOME /usr/verticles

EXPOSE 80

RUN apt-get update \
	&& apt-get install -qy openjdk-21-jdk

COPY target/$VERTICLE_FILE $VERTICLE_HOME/

WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java $JAVA_AGENT -jar $VERTICLE_FILE"]
