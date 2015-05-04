package com.thomsonreuters.rest.service;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

import rx.Notification;
import rx.Observable;
import rx.functions.Func1;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.Modules;
import com.netflix.governator.guice.BootstrapModule;
import com.thomsonreuters.eiddo.EiddoPropertiesLoader;
import com.thomsonreuters.handler.HealthCheck;
import com.thomsonreuters.injection.module.MainModule;
import com.thomsonreuters.rest.service.WebSocketsTest.TestInjectionModule.TestModule;

/**
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

    public static class TestableWebSocketsModule extends KaryonWebSocketsModule<TextWebSocketFrame, TextWebSocketFrame> {
      public TestableWebSocketsModule() {
        super("testWebSocketsModule", TextWebSocketFrame.class, TextWebSocketFrame.class);
      }

      @Override
      protected void configureServer() {
        bindConnectionHandler().to(TestableConnectionHandler.class);
        server().port(PORT);
      }
    }

  }

  private static class TestableConnectionHandler implements ConnectionHandler<TextWebSocketFrame, TextWebSocketFrame> {

    @Configuration("1p.service.name")
    private Supplier<String> appName = Suppliers.ofInstance("One Platform");

    @Inject
    public TestableConnectionHandler() {

    }

    @Override
    public Observable<Void> handle(ObservableConnection<TextWebSocketFrame, TextWebSocketFrame> newConnection) {
      return Observable.interval(INTERVAL, TimeUnit.MILLISECONDS).flatMap(new Func1<Long, Observable<Void>>() {
        @Override
        public Observable<Void> call(Long interval) {
          System.out.println("Writing event for interval: " + interval);
          return newConnection.writeAndFlush(new TextWebSocketFrame("Event " + interval));
        }
      }).materialize().takeWhile(new Func1<Notification<Void>, Boolean>() {
        @Override
        public Boolean call(Notification<Void> notification) {
          if (notification.isOnError()) {
            System.out.println("Write to client failed, stopping response sending.");
            notification.getThrowable().printStackTrace(System.err);
          }
          return !notification.isOnError();
        }
      }).map(new Func1<Notification<Void>, Void>() {
        @Override
        public Void call(Notification<Void> notification) {
          return null;
        }
      });
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
