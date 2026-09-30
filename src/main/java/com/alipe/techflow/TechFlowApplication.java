package com.alipe.techflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TechFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(TechFlowApplication.class, args);
    }
}
