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

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.AvailablePortFinder;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public abstract class WsProducerTestBase extends Assert {
    
    protected static final String TEST_MESSAGE = "Hello World!";
    
    protected CamelContext camelContext;
    protected ProducerTemplate template;
    protected Server server;
    protected int PORT = AvailablePortFinder.getNextAvailable();
    protected List<Object> messages;
    
    public void startTestServer() throws Exception {
        // start a simple websocket echo service
        server = new Server();
        Connector connector = getConnector();
        connector.setHost("localhost");
        connector.setPort(PORT);
        server.addConnector(connector);
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
 
        messages = new ArrayList<Object>();
        server.setHandler(context);
        ServletHolder servletHolder = new ServletHolder(new TestServlet(messages));
        context.addServlet(servletHolder, "/*");
        
        server.start();
        assertTrue(server.isStarted());
        
    }
    
    public void stopTestServer() throws Exception {
        server.stop();
        server.destroy();
    }
    
    @Before
    public void setUp() throws Exception {
        startTestServer();
        
        camelContext = new DefaultCamelContext();
        camelContext.start();
        
        setUpComponent();
        template = camelContext.createProducerTemplate();
    }

    @After
    public void tearDown() throws Exception {
        template.stop();
        camelContext.stop();
        
        stopTestServer();
    }

    protected abstract void setUpComponent() throws Exception;

    protected abstract Connector getConnector() throws Exception;

    protected abstract String getTargetURL();
    
    @Test
    public void testWriteToWebsocket() throws Exception {
        testWriteToWebsocket(TEST_MESSAGE);

    }

    @Test
    public void testWriteBytesToWebsocket() throws Exception {
        testWriteToWebsocket(TEST_MESSAGE.getBytes("utf-8"));
    }

    private void testWriteToWebsocket(Object message) throws Exception {
        Exchange exchange = sendMessage(getTargetURL(), message);
        assertNull(exchange.getException());
        
        long towait = 5000;
        while (towait > 0) {
            if (messages.size() == 1) {
                break;
            }
            towait -= 500;
            Thread.sleep(500);
        }
        assertEquals(1, messages.size());
        verifyMessage(message, messages.get(0));
    }

    private Exchange sendMessage(String endpointUri, final Object msg) {
        Exchange exchange = template.request(endpointUri, new Processor() {
            public void process(final Exchange exchange) {
                exchange.getIn().setBody(msg);
            }
        });
        return exchange;

    }

    private void verifyMessage(Object original, Object result) {
        if (original instanceof String) {
            assertEquals(original, result);
        } else if (original instanceof byte[]) {
            // use string-equals as our bytes are string'able
            assertEquals(new String((byte[])original), new String((byte[])result));
        } else {
            fail("unexpected messages: " + original + " -> " + result);
        }
    }

}
