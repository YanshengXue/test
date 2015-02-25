package com.thomsonreuters.karyon;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;

public abstract class ServletInjectionConfig<T> extends GuiceServletContextListener implements BootstrapAware<T> {
	private final static String APP_NAME_OVERRIDE = "appNameOverride";
	
	
    protected static final Logger log = LoggerFactory.getLogger(ServletInjectionConfig.class);

    private LifecycleManager lifecycleManager;
    private Injector injector;
	
    private void stopLifecycleManager() {
    	if( lifecycleManager != null ) {
    		log.info("Closing Lifecycle Manager");
    		lifecycleManager.close();
    	}
    }
    
    private void startLifecycleManager() {
        lifecycleManager = injector.getInstance(LifecycleManager.class);
        try {
        	log.info("Starting Lifecycle Manager");
            lifecycleManager.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
	
	@Override
	protected Injector getInjector() {
		return injector;
	}

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
    	
    	log.info("contextInitializing");

    	final ServletContext ctx = servletContextEvent.getServletContext();
    	
    	final String appNameOverride = ctx.getInitParameter( APP_NAME_OVERRIDE );
    	
    	if( appNameOverride != null && !appNameOverride.isEmpty() ) {
    		ConfigurationManager.getDeploymentContext().setApplicationId( appNameOverride );
    	}
    	
        injector = LifecycleInjector.bootstrap( getBootstrap() );
        
        log.info("contextInitialized");
        
        try {
        	startLifecycleManager();
        	
        } catch (Exception e) {
            log.error("Error while starting karyon.", e);
            throw Throwables.propagate(e);
        }
        
        super.contextInitialized(servletContextEvent);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        super.contextDestroyed(servletContextEvent);

        try {
        	stopLifecycleManager();
        } catch (Exception e) {
            log.error("Error while stopping karyon.", e);
            throw Throwables.propagate(e);
        }
    }
}
