package com.ragdemo.controller;

import com.ragdemo.common.Result;
import com.ragdemo.dto.request.LoginRequest;
import com.ragdemo.dto.request.RegisterRequest;
import com.ragdemo.dto.response.AuthUserVO;
import com.ragdemo.dto.response.LoginResponse;
import com.ragdemo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "鉴权")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "注册（演示：内存用户）")
    public Result<AuthUserVO> register(@Valid @RequestBody RegisterRequest req) {
        return Result.success(authService.register(req.getUsername(), req.getPassword()));
    }

    @PostMapping("/login")
    @Operation(summary = "登录（返回 JWT）")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(authService.login(req.getUsername(), req.getPassword()));
    }
}
