FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder /workspace/target/*.jar app.jar
RUN chown spring:spring /app/app.jar

USER spring
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=mysql
ENV SERVER_PORT=8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

