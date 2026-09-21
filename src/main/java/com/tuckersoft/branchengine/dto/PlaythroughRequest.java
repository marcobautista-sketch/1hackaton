package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaythroughRequest {

    @NotBlank
    @Size(min = 2, max = 40)
    private String playerTag;

    @NotBlank
    private String startNodeCode;
}
