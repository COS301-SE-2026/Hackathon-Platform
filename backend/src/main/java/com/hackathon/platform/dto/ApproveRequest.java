package com.hackathon.platform.dto;

public class ApproveRequest {
  private boolean approve;

  public ApproveRequest() {}

  public boolean isApprove() {
    return approve;
  }

  public void setApprove(boolean approve) {
    this.approve = approve;
  }
}
