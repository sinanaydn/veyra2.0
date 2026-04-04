package com.veyra.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.veyra")
@EntityScan(basePackages = "com.veyra")
@EnableJpaRepositories(basePackages = "com.veyra")
@EnableJpaAuditing
public class VeyraApplication {

    public static void main(String[] args) {
        SpringApplication.run(VeyraApplication.class, args);
    }
}
