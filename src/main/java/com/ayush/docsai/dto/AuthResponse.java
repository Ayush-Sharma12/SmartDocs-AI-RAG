package com.ayush.docsai.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponse {
    String token;
    Long userId;
    String username;
    String email;
}
