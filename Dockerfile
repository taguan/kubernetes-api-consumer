FROM openjdk:11-jdk-slim

VOLUME /bundles
ENV JAVA_OPTS=""
ENV SIMULATOR_BUNDLE_LOCATION=/bundles

ENTRYPOINT exec java $JAVA_OPTS -jar /usr/share/kubernetes-api-consumer.jar --server.port=9000

ARG JAR_FILE
ADD target/kubernetes-api-consumer-0.0.1-SNAPSHOT.jar /usr/share/kubernetes-api-consumer.jar
