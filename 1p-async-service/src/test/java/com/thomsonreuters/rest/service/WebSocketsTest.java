package com.thomsonreuters.rest.service;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ObservableConnection;

import java.util.ArrayList;
import java.util.List;

import netflix.karyon.Karyon;
import netflix.karyon.KaryonBootstrap;
import netflix.karyon.KaryonServer;
import netflix.karyon.ShutdownModule;
import netflix.karyon.archaius.ArchaiusBootstrap;
import netflix.karyon.transport.http.websockets.KaryonWebSocketsModule;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import rx.Observable;
import rx.functions.Func1;

import com.google.inject.Singleton;
import com.netflix.governator.annotations.Modules;
import com.netflix.governator.guice.BootstrapModule;
import com.thomsonreuters.handler.HealthCheck;
import com.thomsonreuters.injection.WebSocketHandler;
import com.thomsonreuters.injection.module.MainModule;
import com.thomsonreuters.rest.service.WebSocketsTest.TestInjectionModule.TestModule;

/**
 * This is a pure functional self-contained test that verifies karion websocket module.
 * @author yurgis
 *
 */
public class WebSocketsTest {
  private static final int TAKE_MAX_EVENT_COUNT = 10;
  private static final int INTERVAL = 100;
  private static final int PORT = 7001;
  private static KaryonServer server;

  @ArchaiusBootstrap()
  @KaryonBootstrap(name = "junit", healthcheck = HealthCheck.class)
  @Singleton
  @Modules(include = { 
      ShutdownModule.class, 
      TestModule.class, 
      TestInjectionModule.TestableWebSocketsModule.class, 
  })
  public interface TestInjectionModule {
    public static class TestModule extends MainModule {

      @Override
      protected void configure() {
      }
    }

    public static class TestableWebSocketsModule extends KaryonWebSocketsModule<WebSocketFrame, WebSocketFrame> {
      public TestableWebSocketsModule() {
        super("testWebSocketsModule", WebSocketFrame.class, WebSocketFrame.class);
      }

      @Override
      protected void configureServer() {
        bindConnectionHandler().to(WebSocketHandler.class);
        server().port(PORT);
      }
    }

  }

  
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    server = Karyon.forApplication(TestInjectionModule.class, (BootstrapModule[]) null);
    server.start();
    System.out.println("Karyon server started");
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    ShutdownUtil.shutdown();
  }

  public WebSocketsTest() {
  }


  @Test
  public void testHello() throws Exception {
    Iterable<String> eventIterable = RxNetty.<TextWebSocketFrame, TextWebSocketFrame> newWebSocketClientBuilder("localhost", PORT)
        .build().connect()
        .flatMap(new Func1<ObservableConnection<TextWebSocketFrame, TextWebSocketFrame>, Observable<String>>() {
          @Override
          public Observable<String> call(ObservableConnection<TextWebSocketFrame, TextWebSocketFrame> connection) {
            return connection.getInput().map(new Func1<TextWebSocketFrame, String>() {
              @Override
              public String call(TextWebSocketFrame frame) {
                return frame.text();
              }
            });
          }
        }).take(TAKE_MAX_EVENT_COUNT).toBlocking().toIterable();

    List<String> events = new ArrayList<String>();
    for (String event : eventIterable) {
      System.out.println("Received event: " + event);
      events.add(event);
    }
    
    Assert.assertEquals(TAKE_MAX_EVENT_COUNT, events.size());
  }

}
