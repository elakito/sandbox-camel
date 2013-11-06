package org.apache.camel.component.wsservlet;

import java.util.UUID;

import org.atmosphere.websocket.WebSocket;
import org.atmosphere.websocket.WebSocketHandler;
import org.atmosphere.websocket.WebSocketProcessor.WebSocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebsocketHandler extends WebSocketHandler {
    private static final transient Logger LOG = LoggerFactory.getLogger(WebSocketHandler.class);
    
    private WebsocketConsumer consumer;
    private WebSocketStore store;
    
    @Override
    public void onByteMessage(WebSocket webSocket, byte[] data, int offset, int length) {
        // TODO Auto-generated method stub
        throw new RuntimeException("not implemented");
    }

    @Override
    public void onClose(WebSocket webSocket) {
        LOG.info("closing websocket");
        store.removeWebSocket(webSocket);
        
        super.onClose(webSocket);
        LOG.info("websocket closed");
    }

    @Override
    public void onError(WebSocket webSocket, WebSocketException t) {
        super.onError(webSocket, t);
        LOG.error("websocket on error", t);
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        LOG.info("opening websocket");
        super.onOpen(webSocket);
        String connectionKey = UUID.randomUUID().toString();
        store.addWebSocket(connectionKey, webSocket);
        LOG.info("websocket opened");
    }

    @Override
    public void onTextMessage(WebSocket webSocket, String data) {
        LOG.info("processing text message {}", data);
        String connectionKey = store.getConnectionKey(webSocket);
        consumer.sendMessage(connectionKey, data);
        LOG.info("text message sent");
    }
    
    public void setConsumer(WebsocketConsumer consumer) {
        this.consumer = consumer;
        this.store = consumer.getEndpoint().getWebSocketStore();
    }

}
