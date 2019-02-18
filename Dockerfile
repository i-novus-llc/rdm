FROM openjdk:11-slim-sid

LABEL maintainer="apatronov@i-novus.ru"
RUN apk add tzdata
ENV TZ=Europe/Moscow

ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","app.jar"]
