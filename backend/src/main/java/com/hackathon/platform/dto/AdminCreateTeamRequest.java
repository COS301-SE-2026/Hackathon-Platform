package com.hackathon.platform.dto;

import java.util.List;

public class AdminCreateTeamRequest {

  private String teamName;
  private List<String> memberEmails;

  public String getTeamName() {
    return teamName;
  }

  public void setTeamName(String teamName) {
    this.teamName = teamName;
  }

  public List<String> getMemberEmails() {
    return memberEmails;
  }

  public void setMemberEmails(List<String> memberEmails) {
    this.memberEmails = memberEmails;
  }
}
