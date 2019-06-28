FROM openjdk:11-jre-slim

ARG JAR_FILE=rhsvc*.jar
RUN apt-get update
RUN apt-get -yq clean
RUN groupadd -g 985 rhsvc && \
    useradd -r -u 985 -g rhsvc rhsvc
USER rhsvc
RUN ls
COPY target/rhsvc*.jar /opt/rhsvc.jar

ENTRYPOINT [ "java", "-jar", "/opt/rhsvc.jar" ]
