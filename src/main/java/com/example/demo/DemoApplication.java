package com.example.demo;

import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

    @SneakyThrows
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        Thread.sleep(1000);
    }
}


