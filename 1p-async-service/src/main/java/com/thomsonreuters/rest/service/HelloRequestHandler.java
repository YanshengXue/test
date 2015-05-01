package com.thomsonreuters.rest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import rx.Observable;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.channel.StringTransformer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;

public class HelloRequestHandler implements RequestHandler<ByteBuf, ByteBuf> {
  private static final Logger log = LoggerFactory.getLogger(HelloRequestHandler.class);

  private final ObjectMapper mapper;

  private static class HelloPOJO {
    @JsonProperty
    final String message = "Hello there!";
  }

  @Inject
  HelloRequestHandler(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
    try {
      response.write(mapper.writeValueAsString(new HelloPOJO()), StringTransformer.DEFAULT_INSTANCE);

      return response.close();
    } catch (Exception e) {
      log.error("Error", e);
    }

    return null;
  }
}
