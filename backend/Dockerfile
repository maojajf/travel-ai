FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS=""

RUN mkdir -p /sdata/travel/logs
COPY --from=build /workspace/target/*.jar /app/travel.jar

EXPOSE 8181
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/travel.jar"]
