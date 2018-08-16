FROM openjdk:8-jre-alpine

LABEL maintainer="apatronov@i-novus.ru"

ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","app.jar"]
