package com.thomsonreuters.injection;

import netflix.adminresources.resources.KaryonWebAdminModule;
import netflix.karyon.KaryonBootstrap;
import netflix.karyon.archaius.ArchaiusBootstrap;
import netflix.karyon.eureka.KaryonEurekaModule;
import netflix.karyon.jersey.blocking.KaryonJerseyModule;
import netflix.karyon.servo.KaryonServoModule;

import com.google.inject.Singleton;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.annotations.Modules;
import com.thomsonreuters.eiddo.EiddoPropertiesLoader;
import com.thomsonreuters.handler.HealthCheck;
import com.thomsonreuters.injection.module.MainModule;
import com.thomsonreuters.karyon.ShutdownModule;

@ArchaiusBootstrap(loader = EiddoPropertiesLoader.class)
@KaryonBootstrap(name = "1p-service", healthcheck = HealthCheck.class)
@Singleton
@Modules(include = {
        ShutdownModule.class,
        KaryonServoModule.class,
        KaryonWebAdminModule.class,
        KaryonEurekaModule.class,
        MainModule.class,
        BootstrapInjectionModule.KaryonRxRouterModuleImpl.class,
})
public interface BootstrapInjectionModule {
  class KaryonRxRouterModuleImpl extends KaryonJerseyModule {
    @Override
    protected void configureServer() {

      int port = 7001;
      if (ConfigurationManager.getConfigInstance().containsKey("server.port")) {
        try {
          port = Integer.parseInt(ConfigurationManager.getConfigInstance().getProperty("server.port").toString());
        } catch (NumberFormatException e) {
        }
      }
      server().port(port).threadPoolSize(200);
    }
  }
}
