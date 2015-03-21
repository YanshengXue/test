package com.thomsonreuters.injection;

import java.io.IOException;

import javax.inject.Inject;

import netflix.karyon.KaryonBootstrap;
import netflix.karyon.archaius.PropertiesLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.ConfigurationManager;
import com.thomsonreuters.eiddo.EiddoClient;

public class ServicePropertiesLoader implements PropertiesLoader {
	  private static final Logger logger = LoggerFactory.getLogger(ServicePropertiesLoader.class);
	  private final String appNameBase;
	  private final EiddoClient eiddoClient;

	  public ServicePropertiesLoader() {
	    this.appNameBase = null;
	    this.eiddoClient = null;
	  }

	  @Inject
	  ServicePropertiesLoader(KaryonBootstrap karyonBootstrap, EiddoClient eiddoClient) {
	    appNameBase = karyonBootstrap.name();
	    this.eiddoClient = eiddoClient; 
	  }

	  public void load() {
	    try {
	      logger.info(String.format("Loading application properties with app id: %s and environment: %s", appNameBase,
	                                ConfigurationManager.getDeploymentContext().getDeploymentEnvironment()));
	      /**
	       * This loads a property file with the name "appName".properties and
	       * "appName"-"env".properties, if found.
	       */
	      ConfigurationManager.loadCascadedPropertiesFromResources(appNameBase);
	      if (eiddoClient != null) {
	        eiddoClient.install(appNameBase);
	      }
	      
	    } catch(IOException e) {
	      logger
	          .error(String
	                     .format("Failed to load properties for application id: %s and environment: %s. This is ok, if you do not have application level properties.",
	                             appNameBase, ConfigurationManager.getDeploymentContext().getDeploymentEnvironment()), e);
	    }
	  }

}
