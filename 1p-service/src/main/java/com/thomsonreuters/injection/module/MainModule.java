package com.thomsonreuters.injection.module;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.guice.LifecycleInjectorBuilderSuite;
public class MainModule extends AbstractModule {
    @Override
    protected void configure() {
       //Guice bindings goes here
    }
    
    public static LifecycleInjectorBuilderSuite asSuite() {
        return new LifecycleInjectorBuilderSuite() {
            @Override
            public void configure(LifecycleInjectorBuilder builder) {
                builder.withAdditionalModules(new MainModule());
            }
        };
    }
}
