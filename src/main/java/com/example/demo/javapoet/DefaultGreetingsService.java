package com.example.demo.javapoet;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

@Slf4j
class DefaultGreetingsService implements GreetingsService {

    DefaultGreetingsService(MessageService messageService) {
        Assert.notNull(messageService, "the messageService constructor argument is null");
    }

    @PostConstruct
    void begin() {
    }

    @Ray
    @Override
    public void english() {
        log.info("yo");
    }

    @Override
    public void chinese() {
        log.info("ni hao");
    }
}
