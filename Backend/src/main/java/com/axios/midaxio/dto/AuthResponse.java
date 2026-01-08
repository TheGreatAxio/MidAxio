package com.axios.midaxio.dto;

public record AuthResponse(
        String token,
        String email,
        String message
) {}