FROM adoptopenjdk/openjdk11-openj9

RUN mkdir /deployments
WORKDIR /deployments

COPY target/lib/* ./lib/
COPY target/*-runner.jar ./app.jar
COPY models ./models

EXPOSE 8080

ENTRYPOINT exec java $JAVA_OPTS -jar ./app.jar -Dquarkus.http.host=0.0.0.0
