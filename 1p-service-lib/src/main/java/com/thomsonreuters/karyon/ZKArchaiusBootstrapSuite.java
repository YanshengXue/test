package com.thomsonreuters.karyon;

import javax.inject.Inject;

import com.netflix.governator.configuration.ArchaiusConfigurationProvider;
import com.netflix.governator.configuration.ConfigurationOwnershipPolicies;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.guice.LifecycleInjectorBuilderSuite;
import com.netflix.karyon.archaius.PropertiesLoader;
import com.thomsonreuters.karyon.archaius.ZKArchaiusBootstrap;
import com.thomsonreuters.karyon.archaius.ZKDefaultPropertiesLoader;
import com.thomsonreuters.karyon.archaius.ZKPropertiesLoader;

public class ZKArchaiusBootstrapSuite implements LifecycleInjectorBuilderSuite {

    private final Class<? extends ZKPropertiesLoader> propertiesLoaderClass;
    private final ZKPropertiesLoader propertiesLoader;
    private final String root;
    private final int    connectTimeout;
    private final int    sessionTimeout;
    
    public ZKArchaiusBootstrapSuite(String appName) {
        this(new ZKDefaultPropertiesLoader(appName));
    }

    public ZKArchaiusBootstrapSuite(ZKPropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
        this.propertiesLoaderClass = null;
        this.root = null;
        this.connectTimeout = -1;
        this.sessionTimeout = -1;
    }

    public ZKArchaiusBootstrapSuite(Class<ZKPropertiesLoader> propertiesLoader) {
        this.propertiesLoaderClass = propertiesLoader;
        this.propertiesLoader = null;
        this.root = null;
        this.connectTimeout = -1;
        this.sessionTimeout = -1;
    }

    @Inject
    ZKArchaiusBootstrapSuite(ZKArchaiusBootstrap zkBootstrap) {
        this.propertiesLoaderClass = zkBootstrap.loader();
        this.propertiesLoader = null;
        this.connectTimeout = zkBootstrap.connectionTimeout();
        this.sessionTimeout = zkBootstrap.sessionTimeout();
        this.root = zkBootstrap.root();
    }

    @Override
    public void configure(LifecycleInjectorBuilder builder) {
        builder.withAdditionalBootstrapModules(new BootstrapModule() {

            @Override
            public void configure(BootstrapBinder bootstrapBinder) {
                if (null != propertiesLoaderClass) {
                    bootstrapBinder.bind(ZKPropertiesLoader.class).to(propertiesLoaderClass).asEagerSingleton();
                } else {
                    bootstrapBinder.bind(ZKPropertiesLoader.class).toInstance(propertiesLoader);
                }
                
                bootstrapBinder.bind(ZKInitializer.class).toInstance( 
                	new ZKInitializer( ZKArchaiusBootstrapSuite.this.root, ZKArchaiusBootstrapSuite.this.connectTimeout, ZKArchaiusBootstrapSuite.this.sessionTimeout ) 
                );
                
                bootstrapBinder.bind(PropertiesInitializer.class).asEagerSingleton();
                
                ArchaiusConfigurationProvider.Builder builder = ArchaiusConfigurationProvider.builder();
                builder.withOwnershipPolicy(ConfigurationOwnershipPolicies.ownsAll());
                bootstrapBinder.bindConfigurationProvider().toInstance(builder.build());
            }
        });
    }

    private static class ZKInitializer {
    	final String root;
    	final int    connectTimeout;
    	final int    sessionTimeout;
    	
    	ZKInitializer( String root, int connectTimeout, int sessionTimeout ) {
    		this.root = root;
    		this.connectTimeout = connectTimeout;
    		this.sessionTimeout = sessionTimeout;
    	}
    }
    
    /**
     * copy from ArchaiusSute
     * This is required as we want to invoke {@link PropertiesLoader#load()} automatically.
     * One way of achieving this is by using {@link @javax.annotation.PostConstruct} but archaius initialization is done
     * in the bootstrap phase and {@link @javax.annotation.PostConstruct} is not invoked in the bootstrap phase.
     */
    private static class PropertiesInitializer {
    	@Inject
        PropertiesInitializer( ZKPropertiesLoader loader, ZKInitializer init) {
            loader.load( init.root, init.connectTimeout, init.sessionTimeout );
        }
    }
}
