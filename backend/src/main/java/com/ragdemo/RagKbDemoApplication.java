package com.ragdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ragdemo.mapper")
public class RagKbDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagKbDemoApplication.class, args);
    }
}
