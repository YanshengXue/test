package com.thomsonreuters.injection;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;

import java.util.concurrent.TimeUnit;

import rx.Notification;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;
import com.netflix.governator.annotations.Configuration;

public class WebSocketHandler implements ConnectionHandler<TextWebSocketFrame, TextWebSocketFrame> {
	private static final int INTERVAL = 100;
	  
	  @Configuration("1p.service.name")
	    private Supplier<String> appName = Suppliers.ofInstance("One Platform");

	    @Inject
	    public WebSocketHandler() {

	    }

//	    @Override
//	    public Observable<Void> handle(final ObservableConnection<TextWebSocketFrame, TextWebSocketFrame> connection) {
//            return connection.getInput().flatMap(new Func1<TextWebSocketFrame, Observable<Void>>() {
//                @Override
//                public Observable<Void> call(TextWebSocketFrame wsFrame) {
//                    TextWebSocketFrame textFrame = (TextWebSocketFrame) wsFrame;
//                    System.out.println("Got message: " + textFrame.text());
//                    return connection.writeAndFlush(new TextWebSocketFrame(textFrame.text().toUpperCase()));
//                    
//                }
//            }).doOnTerminate(new Action0() {
//				
//				@Override
//				public void call() {
//					System.out.print("===fucked up");
//				}
//			});
//        }

	    
	    @Override
	    public Observable<Void> handle(final ObservableConnection<TextWebSocketFrame, TextWebSocketFrame> newConnection) {
	      return Observable.interval(INTERVAL, TimeUnit.MILLISECONDS).flatMap(new Func1<Long, Observable<Void>>() {
	        @Override
	        public Observable<Void> call(Long interval) {
	          System.out.println("Writing event for interval: " + interval);
	          Channel chn = newConnection.getChannel();
	          boolean isActive = chn.isActive();
	          boolean isOpen = chn.isOpen();
	          
	          newConnection.getInput().doOnTerminate(new Action0() {
				@Override
				public void call() {
					 System.out.println("---- fucked up ");
				}
			  });
	          
	          newConnection.getInput().doOnCompleted(new Action0() {
					@Override
					public void call() {
						 System.out.println("---- fucked up ");
					}
				  });
		        
	          newConnection.getInput().doOnSubscribe(new Action0() {
					@Override
					public void call() {
						 System.out.println("---- fucked up ");
					}
				  });
		      
	          
	          boolean isClosed =  newConnection.isCloseIssued();
	          System.out.println("-----isClosed: " + isClosed);
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
