package com.thomsonreuters.rest.service;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Func1;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.thomsonreuters.handler.HandlerUtils;

@Singleton
public class ServerSentEventHandler implements RequestHandler<ByteBuf, ByteBuf> {
  private static final Logger log = LoggerFactory.getLogger(ServerSentEventHandler.class);

  @Inject
  ServerSentEventHandler() {
  }

  @Override
  public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
    //checkpoint logic
    final int startEventId;
    String lastEventId = request.getHeaders().get("Last-Event-ID");
    if (lastEventId != null) {
      startEventId = Integer.parseInt(lastEventId) + 1;
    } else {
      startEventId = 0;
    }
    response.getHeaders().addHeader("Access-Control-Allow-Origin", "*");
    response.getHeaders().add(HttpHeaders.Names.CONTENT_TYPE, "text/event-stream");
    //response.getHeaders().add(HttpHeaders.Names.CONTENT_LENGTH, "-1");
    response.getHeaders().add(HttpHeaders.Names.CACHE_CONTROL, "no-cache");

    
    String name = HandlerUtils.getParam(request, "name");
    int interval = HandlerUtils.getIntParam(request, "interval", 1000);
    return Observable.interval(interval, TimeUnit.MILLISECONDS).flatMap(new Func1<Long, Observable<Void>>() {
      @Override
      public Observable<Void> call(Long index) {
        long currentEventId = startEventId + index;
        log.info("Writing SSE event for interval: " + currentEventId);
        ByteBuf eventId = response.getAllocator().buffer().writeBytes(String.valueOf(currentEventId).getBytes());
        ByteBuf eventType = name == null? null : response.getAllocator().buffer().writeBytes(name.getBytes());
        ByteBuf data = response.getAllocator().buffer().writeBytes(("{message: 'hello " + currentEventId + "'}").getBytes());
        ServerSentEvent event = new ServerSentEvent(eventId, eventType, data);
        
        return response.writeAndFlush(event, new SSETransformer());
      }
    });
  }
}
