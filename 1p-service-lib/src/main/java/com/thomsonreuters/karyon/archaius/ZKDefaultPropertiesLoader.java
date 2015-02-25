package com.thomsonreuters.karyon.archaius;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.AggregatedConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicWatchedConfiguration;
import com.netflix.config.source.ZooKeeperConfigurationSource;
import com.netflix.config.util.ConfigurationUtils;
import com.netflix.karyon.KaryonBootstrap;

public class ZKDefaultPropertiesLoader implements ZKPropertiesLoader {
	private static final Logger log = LoggerFactory.getLogger(ZKDefaultPropertiesLoader.class);

    private final String appName;

    public enum ZKProperties {
    	ZKConnectTimeout("zookeeper.connection.timeout"),
    	ZKSessionTimeout("zookeeper.session.timeout"),
    	ZKConfigurationRoot("zookeeper.config.root.path"),
    	ZKQuorum("zookeeper.config.ensemble"); 
    	
    	private final String key;
    	
    	private ZKProperties( String key ) {
    		this.key = key;
    	}
    	
    	public String getKey() {
    		return key;
    	}
    	public String getEnviromentKey() {
    		return key.replace(".", "_").toUpperCase();
    	}
    }
    
    public ZKDefaultPropertiesLoader() {
        this.appName = null;
    }

    public ZKDefaultPropertiesLoader(String appName) {
        this.appName = appName;
    }

    @Inject
    ZKDefaultPropertiesLoader(KaryonBootstrap karyonBootstrap) {
        appName = karyonBootstrap.name();
    }

    private String getPropertyFileName( String configName, String environment ) {
    	StringBuilder buffer = new StringBuilder( configName );
    	
    	if( environment != null ) {
    		buffer.append("-").append( environment );
    	}
    	
    	return buffer.append(".properties").toString();
    }
    
    private Properties loadCascadedProperties(String configName) throws IOException {
        final String defaultConfigFileName = getPropertyFileName( configName, null );
        
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(defaultConfigFileName);
        if (url == null) {
            throw new IOException("Cannot locate " + defaultConfigFileName + " as a classpath resource.");
        }
        
        Properties props   = ConfigurationManager.getPropertiesFromFile(url);
        String environment = ConfigurationManager.getDeploymentContext().getDeploymentEnvironment();
        if (environment != null && environment.length() > 0) {
            String envConfigFileName = getPropertyFileName( configName, environment );
            url = loader.getResource(envConfigFileName);
            if (url != null) {
                Properties envProps = ConfigurationManager.getPropertiesFromFile(url);
                if (envProps != null) {
                    props.putAll(envProps);
                }
            }
        }
        return props;
    }
    
    private DynamicWatchedConfiguration loadZKProperties(String root, int conTimeout, int sesTimeout) {
        try {
        	//try system environment first
        	String zkConfigEnsemble = ConfigurationManager.getConfigInstance().getString(ZKProperties.ZKQuorum.getEnviromentKey(), "");
        	String zkConfigRootPath = ConfigurationManager.getConfigInstance().getString(ZKProperties.ZKConfigurationRoot.getEnviromentKey(), root);
        	int    connectTimeout   = ConfigurationManager.getConfigInstance().getInt(ZKProperties.ZKConnectTimeout.getEnviromentKey(), conTimeout);
        	int    sessionTimeout   = ConfigurationManager.getConfigInstance().getInt(ZKProperties.ZKSessionTimeout.getEnviromentKey(), sesTimeout);

        	//now cascade to system properties
        	zkConfigEnsemble = ConfigurationManager.getConfigInstance().getString(ZKProperties.ZKQuorum.getKey(), zkConfigEnsemble);
        	zkConfigRootPath = ConfigurationManager.getConfigInstance().getString(ZKProperties.ZKConfigurationRoot.getKey(), zkConfigRootPath);
        	connectTimeout   = ConfigurationManager.getConfigInstance().getInt(ZKProperties.ZKConnectTimeout.getKey(), connectTimeout);
        	sessionTimeout   = ConfigurationManager.getConfigInstance().getInt(ZKProperties.ZKSessionTimeout.getKey(), sessionTimeout);

        	//we have zookeeper config
        	if( !zkConfigEnsemble.isEmpty() && !zkConfigRootPath.isEmpty() ) {
        		log.info("Found zk quorum: " + zkConfigEnsemble + ", configuration root: " + zkConfigRootPath );

        		connectTimeout = Math.max(2000, connectTimeout);
        		sessionTimeout = Math.max(5000, sessionTimeout);
        		
    	        final CuratorFramework client = CuratorFrameworkFactory.newClient(zkConfigEnsemble, 
    	        		sessionTimeout, connectTimeout, new ExponentialBackoffRetry(1000, 3));

    	        client.start();
    	        if ( !client.getZookeeperClient().blockUntilConnectedOrTimedOut() ) {
    	        	throw new RuntimeException("Failed to start curator at " + zkConfigEnsemble);
    	        }
    	        
    	        log.info("Created and started zk client [{}] for ensemble [{}]", client, zkConfigEnsemble);         
    	        if (client.getState() != CuratorFrameworkState.STARTED) {
    	        	throw new RuntimeException("ZooKeeper located at " + zkConfigEnsemble + " is not started.");
    	        }

    	    	final ZooKeeperConfigurationSource zookeeperConfigSource = new ZooKeeperConfigurationSource( client, zkConfigRootPath );
    			zookeeperConfigSource.start();
    	        
    	        return new DynamicWatchedConfiguration( zookeeperConfigSource );
        	}
        	else {
        		log.warn("Zookeeper quorum or configuration root is not set! {quorum: '" + zkConfigEnsemble + "', root: '" + zkConfigRootPath + "'}"  );
        	}
		} 
        catch (Exception ex) {
			log.error("Failed to configure zookeeper. Dynamic properties will not be used", ex);
		}
        
        return null;
    }
    
    private void loadProperties( String appNameToUse, String root, int connectTimeout, int sessionTimeout ) throws IOException {
    	
        final Properties props = loadCascadedProperties( appNameToUse );
        final AbstractConfiguration instance = ConfigurationManager.getConfigInstance();
        final DynamicWatchedConfiguration watched = loadZKProperties( root, connectTimeout, sessionTimeout );
        
        if ( instance instanceof AggregatedConfiguration ) {
        	final AggregatedConfiguration aggregate = (AggregatedConfiguration)instance;
        	
            ConcurrentMapConfiguration config = new ConcurrentMapConfiguration();
            config.loadProperties(props);

            //zookeeper should be first in order to override static properties
            if( watched != null ) {
            	aggregate.addConfiguration( config, "zk dynamic override configuration" );
            }
            
            aggregate.addConfiguration( config, appNameToUse );
            
        } else {
            ConfigurationUtils.loadProperties(props, instance);
        }
    }
    
    @Override
    public void load( String root, int connectTimeout, int sessionTimeout ) {
        String appNameToUse = appName;

        if ( null == appNameToUse || appName.isEmpty() ) {
            appNameToUse = ConfigurationManager.getDeploymentContext().getApplicationId();
        }

        if( root == null || root.isEmpty() ) {
        	root = "/" + appNameToUse + "/config";
        }
        
        try {
            log.info(String.format("Loading application properties with app id: %s and environment: %s",
                                      appNameToUse,
                                      ConfigurationManager.getDeploymentContext().getDeploymentEnvironment()));
            
            loadProperties( appNameToUse, root, connectTimeout, sessionTimeout );
        } catch (IOException e) {
            log.error(String.format(
                    "Failed to load properties for application id: %s and environment: %s. This is ok, if you do not have application level properties.",
                    appNameToUse,
                    ConfigurationManager.getDeploymentContext().getDeploymentEnvironment()), e);
        }
    }
    
    @Override
    public void load() {
    }
}
