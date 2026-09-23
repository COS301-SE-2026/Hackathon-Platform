From eclipse-temurin:21-jdk-jammy

run apt-get update \
    && apt-get install -y --no-install-recommends \
    findutils \
    grep \
    coreutils \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --create-home --uid 10001 runner

ENV HOME=/tmp
USER runner
WORKDIR /workspace