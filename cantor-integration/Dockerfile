FROM maven:3.9-eclipse-temurin-17

WORKDIR /cantor

COPY . .

RUN mvn clean install -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true -Dsource.skip=true

ENV CANTOR_SERVER_HOST=""
ENV CANTOR_SERVER_PORT="7443"

# Wrapper script to run tests from the pre-built jar (no Maven build at runtime)
COPY --chmod=755 <<'EOF' /entrypoint.sh
#!/bin/sh
java -jar /cantor/cantor-integration/target/cantor-integration.jar "$@"
EOF

ENTRYPOINT ["/entrypoint.sh"]
