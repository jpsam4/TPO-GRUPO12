# Build de la app en dos etapas: compila con Maven, corre solo con el JRE.
# No hace falta tener Java ni Maven instalados para construir esta imagen.

# ---------- etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Primero el pom solo, para que Docker cachee las dependencias y no las
# vuelva a bajar cada vez que cambia una linea de codigo.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario sin privilegios: si alguien escapa de la app, no es root.
RUN useradd --system --uid 1001 spring
COPY --from=build /build/target/*.jar app.jar
USER spring

EXPOSE 8080

# Render (y la mayoria de los PaaS) inyectan el puerto en $PORT.
# Localmente no existe esa variable, asi que cae en 8080.
# Se pasa por linea de comandos para NO tener que tocar application.properties.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]
