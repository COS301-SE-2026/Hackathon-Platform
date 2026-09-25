FROM eclipse-temurin:21-jdk-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
    coreutils \
    findutils \
    grep \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 10001 runner \
    && mkdir -p /workspace \
    && chown -R runner:runner /workspace

ENV HOME=/tmp
USER runner
WORKDIR /workspace