package com.hackathon.platform.scoring;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


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

  
}
