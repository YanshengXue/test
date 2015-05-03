package com.thomsonreuters.injection;

import io.netty.buffer.ByteBuf;
import netflix.adminresources.resources.KaryonWebAdminModule;
import netflix.karyon.KaryonBootstrap;
import netflix.karyon.archaius.ArchaiusBootstrap;
import netflix.karyon.eureka.KaryonEurekaModule;
import netflix.karyon.servo.KaryonServoModule;
import netflix.karyon.transport.http.KaryonHttpModule;

import com.google.inject.Inject;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.annotations.Modules;
import com.sun.jersey.spi.resource.Singleton;
import com.thomsonreuters.eiddo.EiddoPropertiesLoader;
import com.thomsonreuters.events.karyon.EventsModule;
import com.thomsonreuters.handler.HealthCheck;
import com.thomsonreuters.injection.module.MainModule;
import com.thomsonreuters.karyon.ShutdownModule;

@ArchaiusBootstrap(loader = EiddoPropertiesLoader.class)
@KaryonBootstrap(name = "1p-async-service", healthcheck = HealthCheck.class)
@Singleton
@Modules(include = {
        ShutdownModule.class,
        KaryonServoModule.class,
        KaryonWebAdminModule.class,
        KaryonEurekaModule.class,
        EventsModule.class,
        MainModule.class,
        BootstrapInjectionModule.KaryonRxRouterModuleImpl.class,
})
public interface BootstrapInjectionModule {
  class KaryonRxRouterModuleImpl extends KaryonHttpModule<ByteBuf, ByteBuf> {

    @Inject
    public KaryonRxRouterModuleImpl() {
      super("karyonAsyncModule", ByteBuf.class, ByteBuf.class);
    }
    
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
      
      server().port( port );
    }

    
    @Override
    protected void configure() {
      bindRouter().to(HystrixStreamAwareHandler.class);
      super.configure();
    }
    
  }
  
  
}
