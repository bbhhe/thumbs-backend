package com.bbhhe.thumbsbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
@MapperScan("com.bbhhe.thumbsbackend.mapper")
public class ThumbsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThumbsBackendApplication.class, args);
    }

}
