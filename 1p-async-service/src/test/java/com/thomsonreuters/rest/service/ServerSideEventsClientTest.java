package com.thomsonreuters.rest.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ContentTransformer;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;

import java.nio.charset.Charset;
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

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Modules;
import com.netflix.governator.guice.BootstrapModule;
import com.thomsonreuters.handler.HealthCheck;
import com.thomsonreuters.injection.AppRouter;
import com.thomsonreuters.injection.module.MainModule;
import com.thomsonreuters.rest.service.ServerSideEventsClientTest.TestInjectionModule.TestModule;

/**
 * This is partly integration test as it verified app's router and hello resource
 * @author yurgis
 *
 */
public class ServerSideEventsClientTest {
  private static final int INTERVAL = 100;
  private static final int MAX_EVENT_COUNT = 10;
  private static final int PORT = 7001;
  private static KaryonServer server;

  @ArchaiusBootstrap()
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

    class KaryonSSEModuleImpl extends KaryonHttpModule<ByteBuf, ByteBuf> {

      private final AppRouter appRouter;
      
      @Inject
      public KaryonSSEModuleImpl(AppRouter appRouter) {
        super("karyonSSEModule", ByteBuf.class, ByteBuf.class);
        this.appRouter = appRouter;
      }

      @Override
      protected void configureServer() {
        server().port(PORT);
      }

      @Override
      protected void configure() {
        bindRouter().toInstance(new RequestHandler<ByteBuf, ByteBuf>() {

          @Override
          public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
              HttpServerResponse<ByteBuf> response) {
            String uri = request.getUri();
            if ("/sse".equals(uri)) {
              response.getHeaders().add(HttpHeaders.Names.CONTENT_TYPE, "text/eventstream");
              return Observable.interval(INTERVAL, TimeUnit.MILLISECONDS).flatMap(new Func1<Long, Observable<Void>>() {
                @Override
                public Observable<Void> call(Long interval) {
                  System.out.println("Writing SSE event for interval: " + interval);
                  ByteBuf data = response.getAllocator().buffer().writeBytes(("hello " + interval).getBytes());
                  ServerSentEvent event = new ServerSentEvent(data);
                  
                  return response.writeAndFlush(event, new SSETransformer());
                }
              });
            } else {
              return appRouter.handle(request, response);
            }
          }
        });
        super.configure();
      }

      private static class SSETransformer implements ContentTransformer<ServerSentEvent> {
        private static final byte[] EVENT_PREFIX_BYTES = "event: ".getBytes();
        private static final byte[] NEW_LINE_AS_BYTES = "\n".getBytes();
        private static final byte[] ID_PREFIX_AS_BYTES = "id: ".getBytes();
        private static final byte[] DATA_PREFIX_AS_BYTES = "data: ".getBytes();
        
        @Override
        public ByteBuf call(ServerSentEvent serverSentEvent, ByteBufAllocator byteBufAllocator) {
          ByteBuf out = byteBufAllocator.buffer(serverSentEvent.content().capacity());
          if (serverSentEvent.hasEventType()) { // Write event type, if available
            out.writeBytes(EVENT_PREFIX_BYTES);
            out.writeBytes(serverSentEvent.getEventType());
            out.writeBytes(NEW_LINE_AS_BYTES);
          }
  
          if (serverSentEvent.hasEventId()) { // Write event id, if available
            out.writeBytes(ID_PREFIX_AS_BYTES);
            out.writeBytes(serverSentEvent.getEventId());
            out.writeBytes(NEW_LINE_AS_BYTES);
          }
          
          out.writeBytes(DATA_PREFIX_AS_BYTES);
          out.writeBytes(serverSentEvent.content());
          out.writeBytes(NEW_LINE_AS_BYTES);
          
          System.out.println("sent as: '" + out.toString(Charset.forName("UTF-8")) + "'");
          return out;
       }
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

  public ServerSideEventsClientTest() {
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
  public void testSSE() throws Exception {
    System.out.println("Testing new SSE decoder. Server port: " + PORT);
    Iterable<ServerSentEvent> eventIterable = RxNetty
        .<ByteBuf, ServerSentEvent> newHttpClientBuilder("localhost", PORT)
        .pipelineConfigurator(PipelineConfigurators.<ByteBuf> clientSseConfigurator()).build()
        .submit(HttpClientRequest.createGet("/sse"))
        .flatMap(new Func1<HttpClientResponse<ServerSentEvent>, Observable<ServerSentEvent>>() {
          @Override
          public Observable<ServerSentEvent> call(HttpClientResponse<ServerSentEvent> response) {
            printResponseHeader(response);
            return response.getContent().doOnNext(new Action1<ServerSentEvent>() {
              @Override
              public void call(ServerSentEvent serverSentEvent) {
                serverSentEvent.retain();
              }
            });
          }
        }).take(MAX_EVENT_COUNT).toBlocking().toIterable();

    for (ServerSentEvent event : eventIterable) {
      System.out.println("Received sse '" + event.contentAsString() + "'");
      event.release();
    }
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
  }
}
