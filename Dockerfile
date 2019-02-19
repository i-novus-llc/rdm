FROM adoptopenjdk/openjdk11:alpine-slim

RUN apk --no-cache add tzdata ttf-dejavu
ENV TZ=Europe/Moscow


ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","app.jar"]
