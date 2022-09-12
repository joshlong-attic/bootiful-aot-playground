package com.example.demo.javapoet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

@Slf4j
class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof BeanDefinitionRegistry bdr) {
            log.info("the beanFactory is a BeanDefinitionRegistry [" + bdr.getClass().getName() + "]");
        }
        var beanNames = beanFactory.getBeanDefinitionNames();
        for (var beanName : beanNames) {
            var bd = beanFactory.getBeanDefinition(beanName);
            log.debug("bd: " + bd);
        }
    }
}
