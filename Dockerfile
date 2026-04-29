FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar /app/app.jar

# Keep the application code unchanged while routing file uploads to a Render disk.
RUN mkdir -p /render-data/uploads /render-data/assignment-files \
    && rm -rf /app/uploads /app/assignment-files \
    && ln -s /render-data/uploads /app/uploads \
    && ln -s /render-data/assignment-files /app/assignment-files

EXPOSE 8080

CMD ["sh", "-c", "mkdir -p /render-data/uploads /render-data/assignment-files && java -jar /app/app.jar"]
