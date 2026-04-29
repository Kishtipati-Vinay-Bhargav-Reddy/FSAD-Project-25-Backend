FROM openjdk:17-jdk-slim

WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw clean package

EXPOSE 10000

CMD ["java", "-Dserver.port=10000", "-jar", "target/*.jar"]