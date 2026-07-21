package com.ragdemo.dto.response;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;
    private String username;
    private Long expiresIn;
}
