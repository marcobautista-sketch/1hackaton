package com.tuckersoft.branchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class RealityLogResponse {
    private Long id;
    private Long decisionId;
    private String recipientEmail;
    private String subject;
    private String logStatus;
    private String errorMessage;
    private Instant sentAt;
    private Instant createdAt;
}
