package com.hackathon.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateAnnouncementResponse {
    private AnnouncementResponse announcement;
    private int emailRecipientCount;
    private String emailStatus;
}
