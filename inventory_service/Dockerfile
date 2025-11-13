FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.io.tmpdir=/app/.tmp \
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006"
RUN mkdir -p /app/.tmp /app/.tomcat
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8082 5006
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar --server.tomcat.basedir=/app/.tomcat"]
