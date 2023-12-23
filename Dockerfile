FROM openjdk:11.0.5-jre-stretch
MAINTAINER Richard Peters <rpeters@fullfacing.com>

COPY application.jar /app/bin/service.jar
ENTRYPOINT [ "sh", "-c", "java -jar /app/bin/service.jar" ]