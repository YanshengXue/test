package com.thomsonreuters.injection;

import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.Inject;
import com.netflix.governator.annotations.Configuration;

public class WebSocketHandler implements ConnectionHandler<WebSocketFrame, WebSocketFrame> {
	private static final int INTERVAL = 1000;
	class StartEvent{
		String eventId = "0";
		public StartEvent(){
		}
		public StartEvent(String eventId){
			this.eventId = eventId;
		}
		public String getEventId() {
			return eventId;
		}
		public void setEventId(String eventId) {
			this.eventId = eventId;
		}
	}
	
	  @Configuration("1p.service.name")
	    private Supplier<String> appName = Suppliers.ofInstance("One Platform");

	    @Inject
	    public WebSocketHandler() {

	    }

	    @Override
	    public Observable<Void> handle(final ObservableConnection<WebSocketFrame, WebSocketFrame> connection) {
            
	    	final StartEvent startEvent = new StartEvent();
	    	   
	    	Observable<Long> timerObs = Observable.interval(INTERVAL, TimeUnit.MILLISECONDS);
	    	final Observable<Void> obsEvents =  timerObs.flatMap(new Func1<Long, Observable<Void>>() {
	    	        @Override
	    	        public Observable<Void> call(Long interval) {
	    	          System.out.println("Writing event for interval: " + interval);
	    	          System.out.println(startEvent.getEventId());
	    	          String ev = startEvent.getEventId();
	    	          Long adjInterval = Long.valueOf(startEvent.getEventId()) + interval.longValue();
	    	          
	    	          return connection.writeAndFlush(new TextWebSocketFrame(String.valueOf(adjInterval)));
	    	        }
	    	      }).doOnTerminate(new Action0() {
					@Override
					public void call() {
						System.out.print("===fucked up Event");
					}
				});
	    	
	    	Observable<Void> obsIn =  connection.getInput().flatMap(new Func1<WebSocketFrame, Observable<Void>>() {
                @Override
                public Observable<Void> call(WebSocketFrame wsFrame) {
                    if(! (wsFrame instanceof TextWebSocketFrame)){
                        CloseWebSocketFrame  clFr = (CloseWebSocketFrame) wsFrame; 
                        return connection.flush();
                        
                    	//throw new RuntimeException("Socket is closed");
                    }
                	TextWebSocketFrame textFrame = (TextWebSocketFrame) wsFrame;
                    startEvent.setEventId(textFrame.text());
                	System.out.println("Got message: " + textFrame.text());
                    //return connection.writeAndFlush(new TextWebSocketFrame(textFrame.text().toUpperCase()));
                    return connection.flush();
                }
            }).takeUntil(new Func1<Void, Boolean>() {

				@Override
				public Boolean call(Void t) {
					// TODO Auto-generated method stub
					return !connection.isCloseIssued();
				}

				
			}).doOnTerminate(new Action0() {
				
				@Override
				public void call() {
					System.out.print("===fucked up on inputp");
				}
			}).doOnUnsubscribe(new Action0() {
				
				@Override
				public void call() {
					System.out.print("===fucked up on unsubsribe ");
					
				}
			});
        
	    
          
             //return Observable.merge(obsIn, obsEvents);	
	    	return obsIn.mergeWith(obsEvents);
	    }

	    
//	    @Override
//	    public Observable<Void> handle(final ObservableConnection<TextWebSocketFrame, TextWebSocketFrame> newConnection) {
//	      return Observable.interval(INTERVAL, TimeUnit.MILLISECONDS).flatMap(new Func1<Long, Observable<Void>>() {
//	        @Override
//	        public Observable<Void> call(Long interval) {
//	          System.out.println("Writing event for interval: " + interval);
//	          Channel chn = newConnection.getChannel();
//	          boolean isActive = chn.isActive();
//	          boolean isOpen = chn.isOpen();
//	          
//	          newConnection.getInput().doOnTerminate(new Action0() {
//				@Override
//				public void call() {
//					 System.out.println("---- fucked up ");
//				}
//			  });
//	          
//	          newConnection.getInput().doOnCompleted(new Action0() {
//					@Override
//					public void call() {
//						 System.out.println("---- fucked up ");
//					}
//				  });
//		        
//	          newConnection.getInput().doOnSubscribe(new Action0() {
//					@Override
//					public void call() {
//						 System.out.println("---- fucked up ");
//					}
//				  });
//		      
//	          
//	          boolean isClosed =  newConnection.isCloseIssued();
//	          System.out.println("-----isClosed: " + isClosed);
//	          return newConnection.writeAndFlush(new TextWebSocketFrame("Event " + interval));
//	        }
//	      }).materialize().takeWhile(new Func1<Notification<Void>, Boolean>() {
//	        @Override
//	        public Boolean call(Notification<Void> notification) {
//	          if (notification.isOnError()) {
//	            System.out.println("Write to client failed, stopping response sending.");
//	            notification.getThrowable().printStackTrace(System.err);
//	          }
//	          return !notification.isOnError();
//	        }
//	      }).map(new Func1<Notification<Void>, Void>() {
//	        @Override
//	        public Void call(Notification<Void> notification) {
//	          return null;
//	        }
//	      });
//	    }

}
