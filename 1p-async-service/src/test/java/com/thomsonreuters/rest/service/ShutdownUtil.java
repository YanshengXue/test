package com.thomsonreuters.rest.service;

import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;

public class ShutdownUtil {
  
  public static void shutdown() {
    System.out.println("Karyon server shutting down...");
    RxNetty.createTcpClient("localhost", 7002, PipelineConfigurators.stringMessageConfigurator()).connect()
        .take(1)
        .flatMap((observableConnection) -> {
          return observableConnection.writeAndFlush("shutdown\n").doOnCompleted(() -> observableConnection.close());
        }).toBlocking().forEach((a) -> System.out.println("connection closed"));
    System.out.println("Karyon server has been shut down");
  }
  
}
