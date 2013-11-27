/**
 * 
 */
package org.apache.camel.component.wsservlet;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.atmosphere.websocket.WebSocket;

/**
 *
 */
public class WebsocketProducer extends DefaultProducer {
    
    public WebsocketProducer(WebsocketEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    public WebsocketEndpoint getEndpoint() {
        return (WebsocketEndpoint) super.getEndpoint();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        //TODO support binary data
        String message = in.getMandatoryBody(String.class);

        log.debug("Sending to {}", message);
        if (getEndpoint().isSendToAll()) {
            log.debug("Sending to all -> {}", message);
	    //TODO this sequential sending is temporary. consider using atmosphere's broadcast or async send
            for (WebSocket websocket : getEndpoint().getWebSocketStore().getAllWebSockets()) {
                sendMessage(websocket, message);
            }
        } else {
            // look for connection key and get Websocket
            String connectionKey = in.getHeader(WebsocketConstants.CONNECTION_KEY, String.class);
            if (connectionKey != null) {
                WebSocket websocket = getEndpoint().getWebSocketStore().getWebSocket(connectionKey);
                log.debug("Sending to connection key {} -> {}", connectionKey, message);
                sendMessage(websocket, message);
            } else {
                throw new IllegalArgumentException("Failed to send message to single connection; connetion key not set.");
            }
            
        }
    }

    private void sendMessage(WebSocket websocket, String message) {
        try {
            websocket.write(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
