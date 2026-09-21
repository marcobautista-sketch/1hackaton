package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpdateRequest {

    @NotBlank
    private String role;
}
