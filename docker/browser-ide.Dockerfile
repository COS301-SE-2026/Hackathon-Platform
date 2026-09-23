FROM codercom/code-server:4.137.0
USER root
RUN apt-get update && apt-get install -y --no-install-recommends openjdk-21-jdk-headless && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt/java \
    && ln -s "$(dirname "$(dirname "$(readlink -f /usr/bin/javac)")")" \
    /opt/java/openjdk

RUN rm -f /etc/sudoers.d/nopasswd /usr/local/bin/fixuid && chmod a-s /usr/bin/sudo

ENV HOME=/home/coder
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN mkdir -p \
    /workspace \
    /opt/hackathon/extensions \
    /home/coder/.config/code-server \
    /home/coder/.local/share/code-server/User \
    && chown -R 1000:1000 \
    /workspace \
    /opt/hackathon \
    /home/coder/.config \
    /home/coder/.local

COPY --chown=1000:1000 docker/browser-ide.settings.json \
    /home/coder/.local/share/code-server/User/settings.json

USER 1000:1000

RUN code-server \
    --user-data-dir /home/coder/.local/share/code-server \
    --extensions-dir /opt/hackathon/extensions \
    --install-extension redhat.java@1.56.0 \
    && rm -f /home/coder/.config/code-server/config.yaml

WORKDIR /workspace

EXPOSE 8080

ENTRYPOINT ["/usr/bin/dumb-init", "--", "/usr/bin/code-server", "--auth", "none", "--disable-telemetry", "--user-data-dir", "/home/coder/.local/share/code-server", "--extensions-dir", "/opt/hackathon/extensions", "/workspace"]

CMD ["--bind-addr", "0.0.0.0:8080"]