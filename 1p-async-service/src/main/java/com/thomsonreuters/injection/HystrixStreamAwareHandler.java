package com.thomsonreuters.injection;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.hystrix.contrib.rxnetty.metricsstream.HystrixMetricsStreamHandler;

@Singleton
public class HystrixStreamAwareHandler implements RequestHandler<ByteBuf, ByteBuf> {

  private final HystrixMetricsStreamHandler<ByteBuf, ByteBuf> hystrixStreamHandler;
  
  @Inject 
  public HystrixStreamAwareHandler(AppRouter appRouter) {
    this.hystrixStreamHandler = new HystrixMetricsStreamHandler<ByteBuf, ByteBuf>(appRouter);
  }
  
  @Override
  public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
    return hystrixStreamHandler.handle(request, response);
  }

}
