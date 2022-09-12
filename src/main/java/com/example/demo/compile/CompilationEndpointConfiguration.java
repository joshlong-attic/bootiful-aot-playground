package com.example.demo.compile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.generate.GeneratedClasses;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.javapoet.CodeBlock;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
class CompilationEndpointConfiguration {

	@Bean
	CompilationEndpoint compilationEndpoint() {
		return new CompilationEndpoint();
	}

	@Bean
	CompilationAotProcessor compilationAotProcessor() {
		return new CompilationAotProcessor();
	}

}

// see PersistenceAnnotationBeanPostProcessor for a good example in Spring Boot's code.

/**
 * the idea is that this would run at compile time, see the existing, useless instance of
 * {@linkplain CompilationEndpoint} and replace it with one actually instantiated at
 * compile time.
 */
@Slf4j
@RequiredArgsConstructor
class CompilationAotProcessor implements BeanRegistrationAotProcessor {

	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		var good = CompilationEndpoint.class.isAssignableFrom(registeredBean.getBeanClass());
		if (good)
			log.info("good for " + registeredBean.getBeanClass().getName());
		if (!good)
			return null;
		log.info("looks like you should be good to go...");
		return (ctx, code) -> {
			var generatedClasses = ctx.getGeneratedClasses();
			var methodReference = CompilationEndpointCodeGenerator.generateCompilationEndpoint(generatedClasses);
			code.addInstancePostProcessor(methodReference);
		};
	}

}

class CompilationEndpointCodeGenerator {

	static MethodReference generateCompilationEndpoint(GeneratedClasses generatedClasses) {
		var generatedClass = generatedClasses.getOrAddForFeatureComponent("CompilationEndpoint",
				CompilationEndpoint.class, builder -> builder //
						.addModifiers(Modifier.PUBLIC) //
						.addJavadoc("""

								This method registers {@code CompilationEndpoint}

								@author Josh Long

								""".stripIndent()));

		var generatedMethod = generatedClass //
				.getMethods()//
				.add("apply",
						builder -> builder.addModifiers(Modifier.STATIC, Modifier.PUBLIC)
								.addParameter(RegisteredBean.class, "rb") //
								.addParameter(CompilationEndpoint.class, "inputBean") //
								.returns(CompilationEndpoint.class) //
								.addCode(generateMethodCode()) //
				);

		return MethodReference.ofStatic(generatedClass.getName(), generatedMethod.getName());
	}

	private static CodeBlock generateMethodCode() {
		var outputBeanVariableName = "outputBean";
		return CodeBlock.builder() //
				.addStatement("""
				$T $L = new $T(
				  $T.ofEpochMilli($L),
				  $S
				)
				""".stripIndent().trim(), CompilationEndpoint.class, outputBeanVariableName, CompilationEndpoint.class,
				Instant.class, System.currentTimeMillis() + "L", new File(".").getAbsolutePath())
				.addStatement("return $L", outputBeanVariableName) //
				.build();
	}

}

@Slf4j
@Endpoint(id = "compilation")
class CompilationEndpoint {

	private final Map<String, Object> map = new ConcurrentHashMap<>();

	CompilationEndpoint() {
		this.map.put("message", "No compilation information has been covered");
	}

	CompilationEndpoint(Instant instant, String directoryOfCompilation) {
		var map = Map.of("datetime", (Object) instant, "directory", (Object) directoryOfCompilation);
		this.map.putAll(map);
	}

	@Bean
	@ReadOperation
	public Map<String, Object> greet() {
		return Map.of("compilation", this.map, "now", Instant.now());
	}

}