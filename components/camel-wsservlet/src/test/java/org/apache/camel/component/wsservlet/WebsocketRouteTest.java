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
package org.apache.camel.component.wsservlet;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;

public class WebsocketRouteTest extends WebsocketCamelRouterTestSupport {

    @Test
    public void testWebsocketSingleClient() throws Exception {
        TestClient wsclient = new TestClient("ws://localhost:" + PORT + "/hola", 1);
        wsclient.connect();
        
        wsclient.sendTextMessage("Cerveza");
        
        assertTrue(wsclient.await(10));
        assertEquals(1, wsclient.getReceived().size());
        assertEquals("Hola Cerveza", wsclient.getReceived().get(0));
        wsclient.close();
    }

    @Test
    public void testWebsocketBroadcastClient() throws Exception {
        TestClient wsclient1 = new TestClient("ws://localhost:" + PORT + "/hola2", 2);
        TestClient wsclient2 = new TestClient("ws://localhost:" + PORT + "/hola2", 2);
        wsclient1.connect();
        wsclient2.connect();
        
        wsclient1.sendTextMessage("Gambas");
        wsclient2.sendTextMessage("Calamares");
        
        assertTrue(wsclient1.await(10));
        assertTrue(wsclient2.await(10));
        
        assertEquals(2, wsclient1.getReceived().size());
        
        assertTrue(wsclient1.getReceived().contains("Hola Gambas"));
        assertTrue(wsclient1.getReceived().contains("Hola Calamares"));

        assertEquals(2, wsclient2.getReceived().size());
        assertTrue(wsclient2.getReceived().contains("Hola Gambas"));
        assertTrue(wsclient2.getReceived().contains("Hola Calamares"));

        wsclient1.close();
        wsclient2.close();
    }

    // START SNIPPET: payload
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                from("wsservlet:///hola").to("log:info").process(new Processor() {
                    public void process(final Exchange exchange) throws Exception {
                        String msg = exchange.getIn().getBody(String.class);
                        exchange.getIn().setBody("Hola " + msg); 
                    }
                }).to("wsservlet:///hola");

                from("wsservlet:///hola2").to("log:info").process(new Processor() {
                    public void process(final Exchange exchange) throws Exception {
                        String msg = exchange.getIn().getBody(String.class);
                        exchange.getIn().setBody("Hola " + msg); 
                    }
                }).to("wsservlet:///hola2?sendToAll=true");
            }
        };
    }
    // END SNIPPET: payload

}
