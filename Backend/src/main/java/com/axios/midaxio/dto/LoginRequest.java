package com.axios.midaxio.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email or Username is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password
) {}