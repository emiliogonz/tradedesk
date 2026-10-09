# ---- Build stage: full JDK and Maven, discarded after the build ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Dependencies first: this layer is cached until pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline

# Then the source, which changes on every commit.
COPY src src
RUN ./mvnw -B -q package -DskipTests

# Split the jar into layers ordered from least to most frequently changing.
RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted

# ---- Runtime stage: JRE only, no build tools, no source ----
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]