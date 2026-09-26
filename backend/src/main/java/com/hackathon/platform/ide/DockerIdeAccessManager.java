package com.hackathon.platform.ide;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DockerIdeAccessManager implements IdeAccessManager {
  private final String urlTemplate;

  public DockerIdeAccessManager(@Value("${hackathon.ide.url-template}") String urlTemplate) {
    Objects.requireNonNull(urlTemplate, "IDE URL template is required");

    if (urlTemplate.isBlank()) {
      throw new IllegalArgumentException("IDE URL template cannot be empty");
    }

    this.urlTemplate = urlTemplate;
  }

  @Override
  public String getIdeUrl(IdeContainerSession session) {
    Objects.requireNonNull(session, "IDE container session is required");

    if (session.hostPort() <= 0 || session.hostPort() > 65535) {
      throw new IllegalArgumentException("Invalid IDE host port is: " + session.hostPort());
    }

    return urlTemplate
        .replace("{workspaceId}", session.workspaceId().toString())
        .replace("{port}", Integer.toString(session.hostPort()));
  }
}
