FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 certchain
COPY --from=build /app/target/certchain-backend-0.0.1-SNAPSHOT.jar /app/app.jar
USER 10001
EXPOSE 10000
CMD ["sh", "-c", "exec java -jar /app/app.jar --server.port=${PORT:-10000}"]
