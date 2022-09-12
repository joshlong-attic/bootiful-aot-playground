package com.example.demo.javapoet;

import lombok.SneakyThrows;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.core.DecoratingProxy;

import java.util.ArrayList;
import java.util.Arrays;

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
