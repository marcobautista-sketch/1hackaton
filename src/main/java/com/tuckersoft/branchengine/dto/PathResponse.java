package com.tuckersoft.branchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
public class PathResponse {
    private Long playthroughId;
    private String playerTag;
    private String status;
    private String endingCode;
    private String startNodeCode;
    private String currentNodeCode;
    private List<Step> steps;

    @Getter
    @AllArgsConstructor
    public static class Step {
        private int order;
        private Long decisionId;
        private String fromNodeCode;
        private String toNodeCode;
        private String branchType;
        private String impactLevel;
        private Instant createdAt;
    }
}
