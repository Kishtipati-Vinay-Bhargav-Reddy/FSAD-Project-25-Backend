FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw clean package

EXPOSE 10000

CMD ["sh", "-c", "java -Dserver.port=10000 -jar target/*.jar"]