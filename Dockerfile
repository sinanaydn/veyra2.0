# 1. Aşama: Build
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY veyra-core/pom.xml veyra-core/
COPY veyra-auth/pom.xml veyra-auth/
COPY veyra-user/pom.xml veyra-user/
COPY veyra-vehicle/pom.xml veyra-vehicle/
COPY veyra-rental/pom.xml veyra-rental/
COPY veyra-payment/pom.xml veyra-payment/
COPY veyra-app/pom.xml veyra-app/
RUN mvn dependency:go-offline -q
COPY . .
RUN mvn clean package -DskipTests -q

# 2. Aşama: Runtime
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/veyra-app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
