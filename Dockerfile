FROM openjdk:11-jre-slim

ARG jar
RUN apt-get update
RUN apt-get -yq clean
RUN groupadd -g 985 rhsvc && \
    useradd -r -u 985 -g rhsvc rhsvc
USER rhsvc
COPY $jar /opt/rhsvc.jar
ENV JAVA_OPTS=""
ENTRYPOINT [ "java", "-jar", "/opt/rhsvc.jar" ]

