package com.hackathon.platform.dto;

/** DTO for approving or rejecting a join request. */
public class ApproveRequest {
  private boolean approve;

  /** Default constructor. */
  public ApproveRequest() { }

  /** Returns the approval flag. */
  public boolean isApprove() {
    return approve;
  }

  /** Sets the approval flag (true = approve, false = reject). */
  public void setApprove(boolean approve) {
    this.approve = approve;
  }
}
