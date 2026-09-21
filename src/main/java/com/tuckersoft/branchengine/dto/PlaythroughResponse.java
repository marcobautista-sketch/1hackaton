package com.tuckersoft.branchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class PlaythroughResponse {
    private Long id;
    private String playerTag;
    private String ownerEmail;
    private String startNodeCode;
    private String currentNodeCode;
    private Integer lucidity;
    private Integer controlLevel;
    private String status;
    private String endingCode;
    private Instant createdAt;
    private Instant updatedAt;
}
