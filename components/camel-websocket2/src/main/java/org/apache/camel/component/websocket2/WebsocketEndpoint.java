/**
 * 
 */
package org.apache.camel.component.websocket2;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.camel.component.servlet.ServletEndpoint;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class WebsocketEndpoint extends ServletEndpoint {
    private static final transient Logger LOG = LoggerFactory.getLogger(WebsocketEndpoint.class);
    private WebSocketStore store;
    private boolean sendToAll;
    
    public WebsocketEndpoint(String endPointURI, WebsocketComponent component, URI httpUri, HttpClientParams params, HttpConnectionManager httpConnectionManager,
                             HttpClientConfigurer clientConfigurer) throws URISyntaxException {
        super(endPointURI, component, httpUri, params, httpConnectionManager, clientConfigurer);

        //TODO find a better way of assigning the store
        int idx = endPointURI.indexOf('?');
        String name = idx > -1 ? endPointURI.substring(0, idx) : endPointURI;
        this.store = component.getWebSocketStore(name);
    }
    

    /* (non-Javadoc)
     * @see org.apache.camel.Endpoint#createProducer()
     */
    @Override
    public Producer createProducer() throws Exception {
        return new WebsocketProducer(this);
    }

    /* (non-Javadoc)
     * @see org.apache.camel.Endpoint#createConsumer(org.apache.camel.Processor)
     */
    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new WebsocketConsumer(this, processor);
    }

    /* (non-Javadoc)
     * @see org.apache.camel.IsSingleton#isSingleton()
     */
    @Override
    public boolean isSingleton() {
        return true;
    }


    /**
     * @return the sendToAll
     */
    public boolean isSendToAll() {
        return sendToAll;
    }

    /**
     * @param sendToAll the sendToAll to set
     */
    public void setSendToAll(boolean sendToAll) {
        this.sendToAll = sendToAll;
    }
    
    WebSocketStore getWebSocketStore() {
        return store;
    }
}
