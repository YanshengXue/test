package com.thomsonreuters.rest.service;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import netflix.karyon.Karyon;
import netflix.karyon.KaryonBootstrap;
import netflix.karyon.KaryonServer;
import netflix.karyon.ShutdownModule;
import netflix.karyon.archaius.ArchaiusBootstrap;
import netflix.karyon.transport.http.KaryonHttpModule;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import rx.Notification;
import rx.Observable;
import rx.functions.Func1;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Modules;
import com.netflix.governator.guice.BootstrapModule;
import com.thomsonreuters.eiddo.EiddoPropertiesLoader;
import com.thomsonreuters.handler.HealthCheck;
import com.thomsonreuters.injection.module.MainModule;
import com.thomsonreuters.rest.service.ServerSideEventsClientTest.TestInjectionModule.TestModule;

/**
 * @author yurgis
 *
 */
public class ServerSideEventsClientTest {
  private static final int INTERVAL = 1000;
  private static final int TAKE_MAX_EVENT_COUNT = 100;
  private static final int PORT = 7001;
  private static KaryonServer server;

  @ArchaiusBootstrap(loader = EiddoPropertiesLoader.class)
  @KaryonBootstrap(name = "junit", healthcheck = HealthCheck.class)
  @Singleton
  @Modules(include = { 
      ShutdownModule.class, 
      TestModule.class, 
      TestInjectionModule.KaryonSSEModuleImpl.class, 
  })
  public interface TestInjectionModule {
    public static class TestModule extends MainModule {

      @Override
      protected void configure() {
      }
    }

    class KaryonSSEModuleImpl extends KaryonHttpModule<ByteBuf, ServerSentEvent> {

      @Inject
      public KaryonSSEModuleImpl() {
        super("karyonSSEModule", ByteBuf.class, ServerSentEvent.class);
      }

      @Override
      protected void configureServer() {
        server().port(PORT);
      }

      @Override
      protected void configure() {
        bindRouter().toInstance(new RequestHandler<ByteBuf, ServerSentEvent>() {

          @Override
          public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
              HttpServerResponse<ServerSentEvent> response) {
            return getIntervalObservable(response);
          }
        });
        bindPipelineConfigurator().toInstance(PipelineConfigurators.<ByteBuf>serveSseConfigurator());
        super.configure();
      }

      private Observable<Void> getIntervalObservable(final HttpServerResponse<ServerSentEvent> response) {
        return Observable.interval(INTERVAL, TimeUnit.MILLISECONDS).flatMap(new Func1<Long, Observable<Void>>() {
          @Override
          public Observable<Void> call(Long interval) {
            System.out.println("Writing SSE event for interval: " + interval);
            ByteBuf data = response.getAllocator().buffer().writeBytes(("hello " + interval).getBytes());
            ServerSentEvent event = new ServerSentEvent(data);
            return response.writeAndFlush(event);
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
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    server = Karyon.forApplication(TestInjectionModule.class, (BootstrapModule[]) null);
    server.start();
    System.out.println("Karyon server started");
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    System.out.println("Karyon server shutting down");
    server.shutdown();
  }

  public ServerSideEventsClientTest() {
  }

  public List<ServerSentEvent> readServerSideEvents() {
    HttpClient<ByteBuf, ServerSentEvent> client = RxNetty.createHttpClient("localhost", PORT,
        PipelineConfigurators.<ByteBuf> clientSseConfigurator());

    Iterable<ServerSentEvent> eventIterable = client.submit(HttpClientRequest.createGet("/hello"))
        .flatMap(new Func1<HttpClientResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
          @Override
          public Observable<ServerSentEvent> call(HttpClientResponse<ServerSentEvent> response) {
            printResponseHeader(response);
            return response.getContent();
          }
        }).take(TAKE_MAX_EVENT_COUNT).toBlocking().toIterable();

    List<ServerSentEvent> events = new ArrayList<ServerSentEvent>();
    for (ServerSentEvent event : eventIterable) {
      System.out.println(event);
      events.add(event);
    }

    return events;
  }

  private static void printResponseHeader(HttpClientResponse<ServerSentEvent> response) {
    System.out.println("New response received.");
    System.out.println("========================");
    System.out.println(response.getHttpVersion().text() + ' ' + response.getStatus().code() + ' '
        + response.getStatus().reasonPhrase());
    for (Map.Entry<String, String> header : response.getHeaders().entries()) {
      System.out.println(header.getKey() + ": " + header.getValue());
    }
  }

  @Test
  public void testHello() throws Exception {
    List<ServerSentEvent> events = readServerSideEvents();
    Assert.assertEquals(TAKE_MAX_EVENT_COUNT, events.size());
  }

}
