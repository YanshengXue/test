package com.thomsonreuters.injection;

import com.google.inject.Singleton;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.annotations.Modules;
import com.netflix.karyon.KaryonBootstrap;
import com.netflix.karyon.ShutdownModule;
import com.netflix.karyon.eureka.KaryonEurekaModule;
import com.netflix.karyon.jersey.blocking.KaryonJerseyModule;
import com.thomsonreuters.handler.HealthCheck;
import com.thomsonreuters.injection.module.MainModule;
import com.thomsonreuters.karyon.archaius.ZKArchaiusBootstrap;


@KaryonBootstrap(name = "1p-service-template", healthcheck = HealthCheck.class)
@ZKArchaiusBootstrap(root = "/1p-service-template/config", sessionTimeout = 30000, connectionTimeout = 15000 )
@Singleton
@Modules(include = {
        ShutdownModule.class,
        //KaryonServoModule.class,
        //KaryonWebAdminModule.class,
        KaryonEurekaModule.class,
        MainModule.class,
        BootstrapInjectionModule.KaryonRxRouterModuleImpl.class,
})
public interface BootstrapInjectionModule {
	class KaryonRxRouterModuleImpl extends KaryonJerseyModule {
        @Override
        protected void configureServer() {
        	
	        int port = 7001;
	        if( ConfigurationManager.getConfigInstance().containsKey( "server.port" ) ) {
	        	try {
	        		port = Integer.parseInt( ConfigurationManager.getConfigInstance().getProperty( "server.port" ).toString() );
	        	}
	        	catch( NumberFormatException e ) {
	        	}
	        }
            server().port( port ).threadPoolSize( 200 );
        }
	}
}
