# pet-user/Dockerfile
FROM maven:3.9.14-eclipse-temurin-17 AS builder
WORKDIR /build

#复制 pom（所有模块的 pom 都要，否则 Maven reactor 报错）
COPY pom.xml /build/pom.xml
COPY pet-user/pom.xml /build/pet-user/pom.xml
COPY pet-common/pom.xml /build/pet-common/pom.xml
COPY pet-api/pom.xml /build/pet-api/pom.xml
COPY pet-jwt/pom.xml /build/pet-jwt/pom.xml
COPY pet-gateway/pom.xml /build/pet-gateway/pom.xml
COPY pet-pet/pom.xml /build/pet-pet/pom.xml

RUN mvn dependency:go-offline -pl pet-user -am -B

#复制源码
COPY pet-common/src /build/pet-common/src
COPY pet-api/src    /build/pet-api/src
COPY pet-jwt/src    /build/pet-jwt/src
COPY pet-user/src   /build/pet-user/src

RUN mvn clean package -pl pet-user -am -DskipTests -B

FROM eclipse-temurin:17-jre
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app
COPY --from=builder /build/pet-user/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]