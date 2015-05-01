package com.thomsonreuters.injection.module;

import javax.servlet.ServletContextEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.thomsonreuters.injection.BootstrapInjectionModule;

public class ServletInjectionConfig extends GuiceServletContextListener {
	protected static final Logger log = LoggerFactory.getLogger(ServletInjectionConfig.class);
	
	private LifecycleManager manager = null;
	
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        super.contextInitialized( servletContextEvent );
        
        manager = null;
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        super.contextDestroyed(servletContextEvent);
        
        try {
        	if( manager != null ) {
        		manager.close();
        	}
        } catch (Exception e) {
            log.error("Error while stopping contect.", e);
            throw Throwables.propagate(e);
        }
    }
	
	@Override
	protected Injector getInjector() {
		final Injector injector = LifecycleInjector.bootstrap( BootstrapInjectionModule.class );
		
		manager = injector.getInstance(LifecycleManager.class);
		
		try {
			manager.start();
		} catch (Exception e) {
            log.error("Error while starting LifecycleManager.", e);
            throw Throwables.propagate(e);
		}
		
		return injector;
	}

}
