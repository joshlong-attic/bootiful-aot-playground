package com.example.demo.javapoet;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
class JavaPoetTests {

    private MethodSpec buildSubclassMethodFor(Method method) {
        var newMethodDefinition = MethodSpec
                .methodBuilder(method.getName())
                .addModifiers(Modifier.PUBLIC)
                .returns(method.getReturnType())
                .addAnnotation(Override.class);
        var paramNames = new ArrayList<String>();
        Stream.of(method.getParameters())
                .forEach(parameter -> {
                    paramNames.add(parameter.getName());
                    newMethodDefinition.addParameter(parameter.getType(), parameter.getName());
                });
        var javaMethodBodyCode = String.format("   %s super.$L($L) ".trim(), method.getReturnType().equals(Void.class) ? "" : " return");
        newMethodDefinition.addStatement(javaMethodBodyCode, method.getName(), StringUtils.collectionToDelimitedString(paramNames, ","));
        return newMethodDefinition.build();
    }

    private String random() {
        var uuid = UUID.randomUUID().toString();
        var characters = new StringBuilder();
        for (var c : uuid.toCharArray())
            if (Character.isAlphabetic(c) || Character.isDigit(c))
                characters.append(Character.toUpperCase(c));
        return characters.toString();
    }

    private TypeSpec subclassFor(Class<?> target) {
        Assert.state(!java.lang.reflect.Modifier.isFinal(target.getModifiers()), "we can't subclass a type that's final!");
        var methods = Stream
                .of(ReflectionUtils.getUniqueDeclaredMethods(target))
                .filter(m -> !java.lang.reflect.Modifier.isPrivate(m.getModifiers()) && !ReflectionUtils.isObjectMethod(m))
                .map(this::buildSubclassMethodFor)
                .toList();
        var newType = TypeSpec
                .classBuilder(target.getName() + "__Proxy__" + random())
                .addModifiers(Modifier.PUBLIC);
        Stream.of(target.getInterfaces()).forEach(newType::addSuperinterface);
        newType.superclass(target);
        methods.forEach(newType::addMethod);
        return newType.build();
    }

    @Test
    void contextLoads() {
        var clazzToSubclass = DefaultGreetingsService.class;
        var typeSpec = subclassFor(clazzToSubclass);
        var javaFile = JavaFile.builder(clazzToSubclass.getPackageName(), typeSpec).build();
        System.out.println(javaFile);
    }

}
