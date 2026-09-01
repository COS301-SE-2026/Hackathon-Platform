package com.hackathon.platform.dto;

public class ExtendTimerRequest {
  private int additionalTime;

  public ExtendTimerRequest() {}

  public ExtendTimerRequest(int additionalTime) {
    this.additionalTime = additionalTime;
  }

  public int getAdditionalTime() {
    return additionalTime;
  }

  public void setAdditionalTime(int additionalTime) {
    this.additionalTime = additionalTime;
  }
}
