package com.thomsonreuters.rest.service;


import java.io.IOException;

import javax.ws.rs.core.MediaType;

import netflix.karyon.Karyon;
import netflix.karyon.KaryonBootstrap;
import netflix.karyon.KaryonServer;
import netflix.karyon.archaius.ArchaiusBootstrap;

import org.codehaus.jettison.json.JSONException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Modules;
import com.netflix.governator.guice.BootstrapModule;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.thomsonreuters.handler.HealthCheck;
import com.thomsonreuters.injection.BootstrapInjectionModule;
import com.thomsonreuters.injection.module.MainModule;
import com.thomsonreuters.karyon.ShutdownModule;
import com.thomsonreuters.rest.service.HelloworldResourceTest.TestInjectionModule.TestModule;

/**
 * This is partly integration test as it verified app's router and hello resource in addition to SSE server/client test
 * @author yurgis
 *
 */
public class HelloworldResourceTest extends JerseyTest {
	private static final int PORT = 7001;
	private static final String baseUrl = "http://localhost:" + PORT + "/";
	private static KaryonServer server;

	@ArchaiusBootstrap
	@KaryonBootstrap(name = "junit", healthcheck = HealthCheck.class)
	@Singleton
	@Modules(include = { 
	    ShutdownModule.class, 
			TestModule.class,
			BootstrapInjectionModule.KaryonRxRouterModuleImpl.class, }
	)
	public interface TestInjectionModule {
		public static class TestModule extends MainModule {

			@Override
			protected void configure() {
			}
		}
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		server = Karyon.forApplication(TestInjectionModule.class,
				(BootstrapModule[]) null);
		server.start();
		System.out.println("Karyon server started");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	  ShutdownUtil.shutdown();
	}
	
	public HelloworldResourceTest() {
	    super("com.thomsonreuters.rest.service");
	}

	@Override
	protected AppDescriptor configure() {
		return new WebAppDescriptor.Builder().build();
	}

  @Test
  public void testHello() throws JSONException, JsonProcessingException,
      IOException {
    WebResource webResource = client().resource(baseUrl);
    ClientResponse response = webResource.path("/hello")
        .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    String json = response.getEntity(String.class);
    ObjectMapper m = new ObjectMapper();
    JsonNode root = m.readTree(json);
    Assert.assertNotNull(root);
    System.out.println(json);
    Assert.assertTrue(json.contains("One Platform"));
  }
	
}
