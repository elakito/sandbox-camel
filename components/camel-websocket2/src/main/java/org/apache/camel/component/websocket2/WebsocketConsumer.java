/**
 * 
 */
package org.apache.camel.component.websocket2;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.servlet.ServletConsumer;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.websocket.WebSocketProtocol;

/**
 *
 */
public class WebsocketConsumer extends ServletConsumer {
    private AtmosphereFramework framework;
    
    public WebsocketConsumer(WebsocketEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.framework = new AtmosphereFramework(false, true);

        framework.setUseNativeImplementation(false);
        framework.addInitParameter(ApplicationConfig.WEBSOCKET_SUPPORT, "true");
        framework.addInitParameter(ApplicationConfig.WEBSOCKET_PROTOCOL, WebsocketHandler.class.getName());
        framework.init();
        
        WebSocketProtocol wsp = framework.getWebSocketProtocol();
        if (!(wsp instanceof WebsocketHandler)) {
            //TODO throw exception
        }
        ((WebsocketHandler)wsp).setConsumer(this);
    }

    @Override
    public WebsocketEndpoint getEndpoint() {
        return (WebsocketEndpoint)super.getEndpoint();
    }
    
    void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        framework.doCometSupport(AtmosphereRequest.wrap(request), AtmosphereResponse.wrap(response));
    }

    public void sendMessage(final String connectionKey, final String message) {

        final Exchange exchange = getEndpoint().createExchange();

        // set header and body
        exchange.getIn().setHeader(WebsocketConstants.CONNECTION_KEY, connectionKey);
        exchange.getIn().setBody(message);

        // send exchange using the async routing engine
        getAsyncProcessor().process(exchange, new AsyncCallback() {
            public void done(boolean doneSync) {
                if (exchange.getException() != null) {
                    getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
                }
            }
        });
    }
}
