package com.ragdemo.service;

import com.ragdemo.common.BusinessException;
import com.ragdemo.common.ErrorCode;
import com.ragdemo.common.JwtUtil;
import com.ragdemo.dto.response.AuthUserVO;
import com.ragdemo.dto.response.LoginResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 鉴权服务（M1 演示版：内存用户表）。
 * 仅用于展示 JWT 签发/校验流程，生产环境应替换为数据库 + 密码哈希（BCrypt）。
 */
@Service
public class AuthService {

    private final JwtUtil jwtUtil;
    private final Map<String, String> users = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    public AuthService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public AuthUserVO register(String username, String password) {
        if (username == null || username.length() < 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名至少 3 位");
        }
        if (users.containsKey(username)) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        users.put(username, password);
        AuthUserVO vo = new AuthUserVO();
        vo.setUserId(idSeq.getAndIncrement());
        vo.setUsername(username);
        return vo;
    }

    public LoginResponse login(String username, String password) {
        String stored = users.get(username);
        if (stored == null || !stored.equals(password)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = jwtUtil.generateToken(username);
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setUsername(username);
        resp.setExpiresIn(86400L);
        return resp;
    }
}
