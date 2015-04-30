package com.thomsonreuters.injection;

import netflix.adminresources.resources.KaryonWebAdminModule;
import netflix.karyon.KaryonBootstrap;
import netflix.karyon.archaius.ArchaiusBootstrap;
import netflix.karyon.eureka.KaryonEurekaModule;
import netflix.karyon.servo.KaryonServoModule;

import com.google.inject.Singleton;
import com.netflix.governator.annotations.Modules;
import com.thomsonreuters.eiddo.EiddoPropertiesLoader;
import com.thomsonreuters.events.karyon.EventsModule;
import com.thomsonreuters.handler.HealthCheck;
import com.thomsonreuters.injection.module.MainModule;
import com.thomsonreuters.karyon.KaryonJerseyRouterModule;
import com.thomsonreuters.karyon.ShutdownModule;

@ArchaiusBootstrap(loader = EiddoPropertiesLoader.class)
@KaryonBootstrap(name = "1p-service", healthcheck = HealthCheck.class)
@Singleton
@Modules(include = {
        ShutdownModule.class,
        KaryonServoModule.class,
        KaryonWebAdminModule.class,
        KaryonEurekaModule.class,
        EventsModule.class,
        MainModule.class,
        KaryonJerseyRouterModule.class,
})
public interface BootstrapInjectionModule {
  class KaryonRxRouterModuleImpl extends KaryonJerseyRouterModule {
    
    public KaryonRxRouterModuleImpl() {
    }
    
    @Override
    protected void configureServer() {
      //replace default behavior (port,pool size) if needed
      super.configureServer();
    }
  }
  
  
}
