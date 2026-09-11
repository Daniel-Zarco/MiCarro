# =========================================================
# Etapa de build (Maven + JDK 21)
# =========================================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# Maven Wrapper + descriptor, para cachear dependencias.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

RUN ./mvnw -B -ntp -q dependency:go-offline

# Código fuente y empaquetado de producción (sin tests: se ejecutan en CI).
COPY src/ src/

RUN ./mvnw -B -ntp -q clean package -DskipTests

# =========================================================
# Etapa de runtime (JRE 21, usuario no-root)
# =========================================================
FROM eclipse-temurin:21-jre

RUN groupadd --system spring \
    && useradd --system --gid spring --home-dir /app spring

WORKDIR /app

# Solo el jar final.
COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar

# Perfil de producción. Las credenciales se inyectan por entorno, nunca en la imagen.
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

USER spring

ENTRYPOINT ["java", "-jar", "app.jar"]
