FROM openjdk:17-jdk-slim

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY currency-service/pom.xml ./currency-service/
COPY common/pom.xml ./common/

RUN ./mvnw dependency:go-offline -B

COPY common/src ./common/src/
COPY currency-service/src ./currency-service/src/

RUN ./mvnw clean package -DskipTests -B

FROM openjdk:17-jre-slim

WORKDIR /app

RUN groupadd -r currency && useradd -r -g currency currency

COPY --from=0 /app/currency-service/target/currency-service-*.jar app.jar

RUN chown currency:currency app.jar

USER currency

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]