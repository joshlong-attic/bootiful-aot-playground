package com.example.demo;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.DecoratingProxy;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import javax.lang.model.element.Modifier;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SpringBootApplication
public class DemoApplication {

    @SneakyThrows
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        Thread.sleep(1000);
    }
}

/* todo write some code to inject the target as a variable in the constructor and then override all methods of a given target's various methods   */
@Slf4j
class MyProxyProcessor implements BeanPostProcessor, BeanRegistrationAotProcessor {

    @Override
    public boolean isBeanExcludedFromAotProcessing() {
        return false;
    }

    @Override
    public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean bean) {
        return (context, code) -> {
            var beanClass = bean.getBeanClass();
            var methodsWithAnnotation = detectRayMethods(beanClass);
            if (methodsWithAnnotation.length == 0)
                return;

            var runtimeHints = context.getRuntimeHints();
            ProxyUtils.registerHintsForProxy(beanClass, runtimeHints);
            runtimeHints.reflection().registerType(Ray.class, MemberCategory.values());

            var rayInitGeneratedMethod = code.getMethods().add("rayInit", builder ->
                    builder.addModifiers(Modifier.STATIC)
                            .addParameter(RegisteredBean.class, "rb")
                            .addParameter(DefaultGreetingsService.class, "gs")
                            .returns(DefaultGreetingsService.class)
                            .addCode(CodeBlock.builder()
                                    .addStatement("System.out.println(\"Hello, world\")")
                                    .addStatement("return gs ")
                                    .build())
            );
            code.addInstancePostProcessor(MethodReference.ofStatic(
                    code.getClassName(),
                    rayInitGeneratedMethod.getName()));
        };
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        var methodsWithAnnotation = detectRayMethods(bean.getClass());
        if (methodsWithAnnotation.length > 0) {
            return proxyForRayAnnotation(methodsWithAnnotation, bean);
        }
        return bean;
    }

    private static Method[] detectRayMethods(Class<?> beanClass) {
        return ReflectionUtils.getUniqueDeclaredMethods(beanClass, m -> m.getAnnotation(Ray.class) != null);
    }

    /*  */
    private static Object proxyForRayAnnotation(Method[] methodsWithAnnotation, Object target) {
        log.info("creating a proxy for " + target);
        var pfb = new ProxyFactoryBean();
        pfb.setTarget(target);
        pfb.setInterfaces(target.getClass().getInterfaces());
        pfb.addAdvice((MethodInterceptor) invocation -> {
            var method = invocation.getMethod();
            var methodPredicate = (Predicate<Method>) theMethod -> theMethod.getName().equals(method.getName()) && Arrays.equals(theMethod.getParameterTypes(), method.getParameterTypes()) && theMethod.getReturnType().equals(method.getReturnType());
            var isSameMethod = Stream.of(methodsWithAnnotation).anyMatch(methodPredicate);
            if (isSameMethod) {
                log.info("start [" + method.getName() + "]");
                var res = invocation.proceed();
                log.info("stop [" + method.getName() + "]");
                return res;
            }
            return invocation.proceed();
        });
        return pfb.getObject();
    }
}

abstract class ProxyUtils {

    @SneakyThrows
    public static void registerHintsForProxy(Class<?> beanClass, RuntimeHints hints) {
        var memberCategories = MemberCategory.values();
        var interfaces = beanClass.getInterfaces();
        var reflectionHints = hints.reflection();
        var listOfInterfaces = new ArrayList<Class<?>>();
        listOfInterfaces.addAll(Arrays.asList(interfaces));
        listOfInterfaces.addAll(Arrays.asList(SpringProxy.class, Advised.class, DecoratingProxy.class));
        reflectionHints.registerType(beanClass, memberCategories);
        for (var i : interfaces)
            reflectionHints.registerType(i, memberCategories);
        hints.proxies().registerJdkProxy(listOfInterfaces.toArray(new Class<?>[0]));
    }

}

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


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface Ray {
}

interface GreetingsService {
    void english();

    void chinese();
}


@Slf4j
class DefaultGreetingsService implements   GreetingsService {

    DefaultGreetingsService(MessageService messageService) {
        Assert.notNull(messageService, "the messageService constructor argument is null");
    }

    @PostConstruct
    void begin (){}

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

class DefaultMessageService implements MessageService {
}

interface MessageService {
}