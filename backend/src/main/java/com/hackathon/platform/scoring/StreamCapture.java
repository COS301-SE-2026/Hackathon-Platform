package com.hackathon.platform.scoring;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Drains an InputStream on a background thread up to a byte cap, so a runaway solver can't exhaust
 * heap by printing endlessly. Must be started immediately after process creation.
 */
final class StreamCapture {

  private final Thread thread;
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  private final int maxBytes;

  private StreamCapture(InputStream input, int maxBytes) {
    this.maxBytes = maxBytes;
    this.thread = new Thread(() -> drain(input));
    this.thread.setDaemon(true);
  }

  static StreamCapture read(InputStream input, int maxBytes) {
    StreamCapture capture = new StreamCapture(input, maxBytes);
    capture.thread.start();
    return capture;
  }

  private void drain(InputStream input) {
    byte[] chunk = new byte[4096];
    int n;
    try {
      while ((n = input.read(chunk)) != -1) {
        synchronized (buffer) {
          if (buffer.size() < maxBytes) {
            buffer.write(chunk, 0, Math.min(n, maxBytes - buffer.size()));


          }
        }

      }

    } catch (IOException ignored) {
      // Stream closes when the process exits or is killed;

    }
  }

  String getTextSoFar() {
    synchronized (buffer) {
      return buffer.toString(StandardCharsets.UTF_8);
    }
  }

  
}
