package com.ragdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RAG 知识库问答系统 · 后端启动类。
 * 技术栈：Spring Boot 3 + MyBatis-Plus + PGVector（PostgreSQL 向量扩展）。
 * @MapperScan 让 MyBatis 扫描 com.ragdemo.mapper 下的接口并自动生成实现。
 */
@SpringBootApplication
@MapperScan("com.ragdemo.mapper")
public class RagKbDemoApplication {

    public static void main(String[] args) {
        // 启动 Spring 容器；Flyway 会在此期间自动连库并执行 V1__init_schema.sql 建表
        SpringApplication.run(RagKbDemoApplication.class, args);
    }
}
