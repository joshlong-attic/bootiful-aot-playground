package com.example.demo.javapoet;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class WiringConfiguration {

	@Bean
	static MyBeanFactoryPostProcessor myBeanFactoryPostProcessor() {
		return new MyBeanFactoryPostProcessor();
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> eventApplicationListener(GreetingsService greetingsService) {
		return event -> {
			greetingsService.english();
			greetingsService.chinese();
		};
	}

	@Bean
	MyProxyProcessor myBeanPostProcessor() {
		return new MyProxyProcessor();
	}

	@Bean
	DefaultMessageService messageService() {
		return new DefaultMessageService();
	}

	@Bean
	DefaultGreetingsService greetingsService(MessageService messageService) {
		return new DefaultGreetingsService(messageService);
	}

}
