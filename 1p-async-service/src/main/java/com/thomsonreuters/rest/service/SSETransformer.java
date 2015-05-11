package com.thomsonreuters.rest.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.reactivex.netty.channel.ContentTransformer;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;

import java.nio.charset.Charset;

public class SSETransformer implements ContentTransformer<ServerSentEvent> {
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
    
    out.writeBytes(NEW_LINE_AS_BYTES); //should end with 2 eols
    out.writeBytes(NEW_LINE_AS_BYTES);
    
    System.out.println("sent as: '" + out.toString(Charset.forName("UTF-8")) + "'");
    return out;
 }
}
