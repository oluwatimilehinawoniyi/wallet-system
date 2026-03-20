FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY .mvn ./.mvn
COPY mvnw ./
COPY pom.xml ./
COPY src ./src
RUN chmod +x mvnw && ./mvnw -q -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/target/wallet-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
