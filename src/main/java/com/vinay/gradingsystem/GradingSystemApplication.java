package com.vinay.gradingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.vinay.gradingsystem.repository")
public class GradingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(GradingSystemApplication.class, args);
    }
}
