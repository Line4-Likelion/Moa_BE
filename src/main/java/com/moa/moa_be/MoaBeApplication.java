package com.moa.moa_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class MoaBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoaBeApplication.class, args);
    }

}
