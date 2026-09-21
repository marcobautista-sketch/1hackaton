package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6)
    private String password;

    @NotBlank
    @Size(min = 3, max = 60)
    private String displayName;

    // Nota: el enunciado dice que el registro SIEMPRE crea ROLE_USER, aunque el
    // request incluya un campo "role". Por eso este DTO ni siquiera lo declara:
    // si el cliente lo manda, Jackson lo ignora silenciosamente.
}
