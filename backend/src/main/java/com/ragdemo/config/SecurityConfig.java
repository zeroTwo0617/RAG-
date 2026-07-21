package com.ragdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置（M1 演示）。
 * 当前所有 /api/** 放行，便于直接联调。
 * 后续里程碑接入 JwtAuthenticationFilter 后，将以下方注释的方式开启鉴权：
 *   .requestMatchers("/api/auth/**").permitAll()
 *   .anyRequest().authenticated()
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
