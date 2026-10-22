# syntax=docker/dockerfile:1.7
# =====================================================================================================
# jPrime - production images, built entirely from source (no local JDK / Maven / Node needed)
#
#   docker build --target site  -t jprime-site  .
#   docker build --target admin -t jprime-admin .
#
# Normally you do not call this directly: deploy/docker-compose.yml builds both targets.
# The admin image bundles the React admin UI, so one container serves the API and the UI.
# =====================================================================================================

ARG JAVA_VERSION=21

# ---------- 1. Admin UI (React + Vite) ----------------------------------------------------------------
FROM node:22-alpine AS admin-ui
WORKDIR /build/admin-ui
COPY admin-ui/package.json admin-ui/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY admin-ui/ ./
RUN npm run build

# ---------- 2. Java build (core + site + admin) ------------------------------------------------------
FROM maven:3.9-eclipse-temurin-${JAVA_VERSION} AS build
WORKDIR /build
COPY pom.xml ./
COPY core/pom.xml core/
COPY site/pom.xml site/
COPY admin/pom.xml admin/
COPY core/src core/src
COPY site/src site/src
COPY admin/src admin/src
# Bundle the admin UI: replace the placeholder index.html with the built SPA
COPY --from=admin-ui /build/admin-ui/dist/ admin/src/main/resources/META-INF/resources/
# the ~/.m2 cache mount keeps downloaded dependencies between builds
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp package -DskipTests -pl site,admin -am

# ---------- 3. Runtime base ---------------------------------------------------------------------------
FROM eclipse-temurin:${JAVA_VERSION}-jre-noble AS runtime
# fontconfig + DejaVu: needed by the captcha, image resizing and JasperReports (invoices, tickets)
# curl: container health check; tini: proper PID 1 (signals, zombie reaping)
RUN apt-get update \
 && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core curl tini tzdata \
 && rm -rf /var/lib/apt/lists/* \
 && fc-cache -f \
 && groupadd --system --gid 1001 jprime \
 && useradd --system --uid 1001 --gid jprime --home-dir /deployments --shell /usr/sbin/nologin jprime
ENV LANG=C.UTF-8 \
    TZ=Europe/Sofia \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djava.util.logging.manager=org.jboss.logmanager.LogManager" \
    QUARKUS_HTTP_HOST=0.0.0.0
WORKDIR /deployments
ENTRYPOINT ["/usr/bin/tini", "--", "sh", "-c", "exec java $JAVA_OPTS -jar /deployments/quarkus-run.jar"]

# ---------- 4a. Public site ---------------------------------------------------------------------------
FROM runtime AS site
COPY --from=build --chown=jprime:jprime /build/site/target/quarkus-app/lib/     /deployments/lib/
COPY --from=build --chown=jprime:jprime /build/site/target/quarkus-app/*.jar    /deployments/
COPY --from=build --chown=jprime:jprime /build/site/target/quarkus-app/app/     /deployments/app/
COPY --from=build --chown=jprime:jprime /build/site/target/quarkus-app/quarkus/ /deployments/quarkus/
USER jprime
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://localhost:8080/q/health/ready || exit 1

# ---------- 4b. Admin (API + bundled React UI) --------------------------------------------------------
FROM runtime AS admin
COPY --from=build --chown=jprime:jprime /build/admin/target/quarkus-app/lib/     /deployments/lib/
COPY --from=build --chown=jprime:jprime /build/admin/target/quarkus-app/*.jar    /deployments/
COPY --from=build --chown=jprime:jprime /build/admin/target/quarkus-app/app/     /deployments/app/
COPY --from=build --chown=jprime:jprime /build/admin/target/quarkus-app/quarkus/ /deployments/quarkus/
USER jprime
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://localhost:8081/q/health/ready || exit 1
