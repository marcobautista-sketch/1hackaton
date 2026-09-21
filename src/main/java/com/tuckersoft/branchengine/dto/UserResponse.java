package com.tuckersoft.branchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String displayName;
    private String role;
    private Instant createdAt;
}
