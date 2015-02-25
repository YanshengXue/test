package com.thomsonreuters.karyon.archaius;

import com.netflix.governator.guice.annotations.Bootstrap;
import com.thomsonreuters.karyon.ZKArchaiusBootstrapSuite;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Bootstrap(ZKArchaiusBootstrapSuite.class)
public @interface ZKArchaiusBootstrap {
	String root();
	
	Class<? extends ZKPropertiesLoader> loader() default ZKDefaultPropertiesLoader.class;
	int    sessionTimeout()               default 20000;
	int    connectionTimeout()            default 10000;
}
