package com.thomsonreuters.injection;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import netflix.karyon.transport.http.SimpleUriRouter;
import netflix.karyon.transport.http.health.HealthCheckEndpoint;
import rx.Observable;

import com.google.inject.Inject;
import com.thomsonreuters.handler.HealthCheck;
import com.thomsonreuters.rest.service.HelloRequestHandler;

public class AppRouter implements RequestHandler<ByteBuf, ByteBuf> {
  private final SimpleUriRouter<ByteBuf, ByteBuf> delegate;

  @Inject
  public AppRouter(HealthCheck healthCheck, HelloRequestHandler helloRequest) {
      delegate = new SimpleUriRouter<ByteBuf, ByteBuf>()
        .addUri("/hello",       helloRequest )
        .addUri("/healthcheck", new HealthCheckEndpoint(healthCheck) );
  }

  @Override
  public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
      return delegate.handle(request, response);
  }
}