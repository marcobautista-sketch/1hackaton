package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DecisionRequest {

    @NotNull
    private Long playthroughId;

    @NotBlank
    @Size(min = 10)
    private String rawInput;

    /**
     * Se valida a mano en el service contra LEVE/MODERADO/GRAVE/CRITICO
     * (usar un enum de Jackson aqui rompe la validacion con 400 en vez del
     * mensaje del enunciado).
     */
    @NotBlank
    private String impactLevel;
}
