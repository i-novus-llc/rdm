FROM harbor.i-novus.ru/library/java17-runtime-base:1.0.0 AS builder

WORKDIR /build
ARG JAR_FILE
COPY "${JAR_FILE}" app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM harbor.i-novus.ru/library/java17-runtime-base:1.0.0

EXPOSE 8080

WORKDIR /app
COPY --from=builder /build/dependencies/ ./
COPY --from=builder /build/spring-boot-loader/ ./
COPY --from=builder /build/snapshot-dependencies/ ./
COPY --from=builder /build/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
