package com.axios.midaxio.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private String email;
    private boolean riotLinked;
    private String gameName;
    private String tagLine;
}