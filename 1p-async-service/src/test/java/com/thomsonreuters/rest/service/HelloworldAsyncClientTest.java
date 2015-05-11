package com.thomsonreuters.rest.service;


import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import netflix.karyon.Karyon;
import netflix.karyon.KaryonBootstrap;
import netflix.karyon.KaryonServer;
import netflix.karyon.ShutdownModule;
import netflix.karyon.archaius.ArchaiusBootstrap;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import com.google.inject.Singleton;
import com.netflix.governator.annotations.Modules;
import com.netflix.governator.guice.BootstrapModule;
import com.thomsonreuters.eiddo.EiddoPropertiesLoader;
import com.thomsonreuters.handler.HealthCheck;
import com.thomsonreuters.injection.BootstrapInjectionModule;
import com.thomsonreuters.injection.module.MainModule;
import com.thomsonreuters.rest.service.HelloworldAsyncClientTest.TestInjectionModule.TestModule;

/**
 * This is really an end-to-end test that verifies Eiddo properties are dynamically loaded.
 * @author yurgis
 *
 */
public class HelloworldAsyncClientTest {
	private static final int PORT = 7001;
	private static KaryonServer server;

	@ArchaiusBootstrap(loader = EiddoPropertiesLoader.class)
	@KaryonBootstrap(name = "junit", healthcheck = HealthCheck.class)
	@Singleton
	@Modules(include = { ShutdownModule.class, 
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
	  System.setProperty("eiddo.client.repoChain", "junit");
    System.setProperty("eiddo.repo.junit.urlTemplate", "https://eiddo.1p.thomsonreuters.com/r/junit");
    System.setProperty("eiddo.repo.junit.username", "junit");
    System.setProperty("eiddo.repo.junit.password", "junit");
		server = Karyon.forApplication(TestInjectionModule.class,
				(BootstrapModule[]) null);
		server.start();
		System.out.println("Karyon server started");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
    ShutdownUtil.shutdown();
	}
	
	public HelloworldAsyncClientTest() {
	}

  @Test
  public void testHello() throws Exception {
    HttpClient<ByteBuf, ByteBuf> client = RxNetty.<ByteBuf, ByteBuf> newHttpClientBuilder("localhost", PORT)
        .enableWireLogging(LogLevel.ERROR).build();
    Observable<HttpClientResponse<ByteBuf>> response = client.submit(HttpClientRequest.createGet("/hello"));
    final List<String> result = new ArrayList<String>();
    response.flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<String>>() {
      @Override
      public Observable<String> call(HttpClientResponse<ByteBuf> response) {
        return response.getContent().map(new Func1<ByteBuf, String>() {
          @Override
          public String call(ByteBuf byteBuf) {
            return byteBuf.toString(Charset.forName("UTF-8"));
          }
        });
      }
    }).toBlocking().forEach(new Action1<String>() {

      @Override
      public void call(String t1) {
        result.add(t1);
      }
    });
    Assert.assertEquals("Response not found.", 1, result.size());

    String message = result.get(0);

    Assert.assertTrue("Invalid message from server", 
        message.contains("One Platform"));
    
    Assert.assertTrue("Message must be overriden by Eiddo", 
        message.contains("Overriden by Eiddo"));
  }
	
}
