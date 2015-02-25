package com.thomsonreuters.injection;

import com.netflix.governator.annotations.Modules;
import com.netflix.karyon.KaryonBootstrap;
import com.netflix.karyon.eureka.KaryonEurekaModule;
import com.netflix.karyon.servo.KaryonServoModule;
import com.thomsonreuters.handler.BasicHealthCheck;
import com.thomsonreuters.karyon.ServletInjectionConfig;
import com.thomsonreuters.karyon.archaius.ZKArchaiusBootstrap;

@KaryonBootstrap(name = "", healthcheck = BasicHealthCheck.class)
@ZKArchaiusBootstrap(root = "", sessionTimeout = 30000, connectionTimeout = 15000 )
@Modules(include = {
        //KaryonWebAdminModule.class,
        KaryonServoModule.class,
        KaryonEurekaModule.class,
})
public class BasicInjectionConfig extends ServletInjectionConfig<BasicInjectionConfig> {
	@Override
	public Class<BasicInjectionConfig> getBootstrap() {
		return BasicInjectionConfig.class;
	}
}
