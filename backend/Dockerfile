# Faza 1: build. JDK i Maven su potrebni samo za kompajliranje.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Prvo samo pom.xml i Maven wrapper: sloj sa zavisnostima se kešira
# i ne skida se ponovo dok se pom.xml ne promijeni.
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Git čuva mvnw bez prava izvršavanja (Windows), pa ga ovdje dozvolimo.
# Bez ovoga build pada s "Permission denied".
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

# Tek onda izvorni kod. Testovi se ovdje preskaču jer trebaju bazu;
# pokreću se posebno (lokalno, kasnije u CI-ju).
COPY src src
RUN ./mvnw -q package -DskipTests

# Faza 2: pokretanje. Samo JRE, bez Mavena i izvornog koda.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Aplikacija ne radi kao root: ako je neko provali, ima manje prava
RUN useradd --system --no-create-home appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

# Render (i drugi hostovi) kažu na kojem portu da slušamo preko varijable PORT;
# lokalno je podrazumijevano 8080.
# MaxRAMPercentage ograničava heap da JVM ne pređe limit memorije besplatnog plana.
EXPOSE 8080
ENTRYPOINT exec java -XX:MaxRAMPercentage=70 -Dserver.port=${PORT:-8080} -jar app.jar
