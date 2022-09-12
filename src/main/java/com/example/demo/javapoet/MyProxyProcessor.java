package com.example.demo.javapoet;


import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.ReflectionUtils;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
