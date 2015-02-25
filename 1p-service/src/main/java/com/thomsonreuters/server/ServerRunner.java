package com.thomsonreuters.server;
import java.io.IOException;

import com.netflix.governator.guice.LifecycleInjectorBuilderSuite;
import com.netflix.karyon.Karyon;
import com.thomsonreuters.injection.BootstrapInjectionModule;

public class ServerRunner {
	public static void main( String[] args ) throws IOException {
	
        Karyon
    	.forApplication( BootstrapInjectionModule.class, (LifecycleInjectorBuilderSuite[]) null )
    	.startAndWaitTillShutdown();
	}
}

