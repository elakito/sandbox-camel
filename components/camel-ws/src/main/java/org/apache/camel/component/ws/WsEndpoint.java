/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.ws;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLContext;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class WsEndpoint extends DefaultEndpoint {
    private static final transient Logger LOG = LoggerFactory.getLogger(WsEndpoint.class);
    
    private AsyncHttpClient client;
    private AsyncHttpClientConfig clientConfig;
    private WebSocket websocket;
    private Set<WsConsumer> consumers;
    private URI wsUri;
    private boolean throwExceptionOnFailure = true;
    private boolean transferException;
    private SSLContextParameters sslContextParameters;

    public WsEndpoint(String endpointUri, WsComponent component) {
        super(endpointUri, component);
        this.consumers = new HashSet<WsConsumer>();
    }

    @Override
    public WsComponent getComponent() {
        return (WsComponent) super.getComponent();
    }

    @Override
    public Producer createProducer() throws Exception {
        return new WsProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new WsConsumer(this, processor);
    }


    @Override
    public boolean isSingleton() {
        return true;
    }

    WebSocket getWebSocket() {
        synchronized (this) {
            if (websocket == null) {
                try { 
                    connect();
                } catch (Exception e) {
                    // TODO add the throw exception in the method 
                    e.printStackTrace();
                }
            }
        }
        return websocket;
    }

    void setWebSocket(WebSocket websocket) {
        this.websocket = websocket;
    }

    public AsyncHttpClientConfig getClientConfig() {
        return clientConfig;
    }

    public void setClientConfig(AsyncHttpClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    public boolean isThrowExceptionOnFailure() {
        return throwExceptionOnFailure;
    }

    public void setThrowExceptionOnFailure(boolean throwExceptionOnFailure) {
        this.throwExceptionOnFailure = throwExceptionOnFailure;
    }

    public boolean isTransferException() {
        return transferException;
    }

    public void setTransferException(boolean transferException) {
        this.transferException = transferException;
    }
    
    public SSLContextParameters getSslContextParameters() {
        return sslContextParameters;
    }

    public void setSslContextParameters(SSLContextParameters sslContextParameters) {
        this.sslContextParameters = sslContextParameters;
    }

    public URI getWsUri() {
        return wsUri;
    }

    public void setWsUri(URI wsUri) {
        this.wsUri = wsUri;
    }

    public void connect() throws InterruptedException, ExecutionException, IOException {
        websocket = client.prepareGet(wsUri.toASCIIString()).execute(
            new WebSocketUpgradeHandler.Builder()
            .addWebSocketListener(new WebSocketTextListener() {
                @Override
                public void onMessage(String message) {
                    LOG.info("received message --> {}", message);
                    for (WsConsumer consumer : consumers) {
                        consumer.sendMessage(message);
                    }
                }
                
                @Override
                public void onFragment(String fragment, boolean last) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("received fragment({}) --> {}", last, fragment);
                    }
                    // for now, ignore the fragment mode
                }
                
                @Override
                public void onOpen(WebSocket websocket) {
                    LOG.info("websocket opened");
                }
                
                @Override
                public void onClose(WebSocket websocket) {
                    LOG.info("websocket closed");
                }
                
                @Override
                public void onError(Throwable t) {
                    //TODO provide a reconnect option for permanent error
                    LOG.error("websocket on error", t);
                }
            }).build()).get();
    }
    
    
    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (client == null) {
            
            AsyncHttpClientConfig config = null;
            
            if (clientConfig != null) {
                AsyncHttpClientConfig.Builder builder = WsComponent.cloneConfig(clientConfig);
                
                if (sslContextParameters != null) {
                    SSLContext ssl = sslContextParameters.createSSLContext();
                    builder.setSSLContext(ssl);
                }
                
                config = builder.build();
            } else {
                if (sslContextParameters != null) {
                    AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
                    SSLContext ssl = sslContextParameters.createSSLContext();
                    builder.setSSLContext(ssl);
                    config = builder.build();
                }
            }
            
            if (config == null) {
                client = new AsyncHttpClient();
            } else {
                client = new AsyncHttpClient(config);
            }
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (websocket != null && websocket.isOpen()) {
            websocket.close();
        }
        if (client != null && !client.isClosed()) {
            client.close();
        }
        client = null;
    }

    void connect(WsConsumer wsConsumer) {
        consumers.add(wsConsumer);
    }

    void disconnect(WsConsumer wsConsumer) {
        consumers.remove(wsConsumer);
    }

}
