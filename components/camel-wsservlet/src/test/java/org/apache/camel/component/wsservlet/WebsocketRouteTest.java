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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;

public class WebsocketRouteTest extends WebsocketCamelRouterTestSupport {
    private static final String RESPONSE_GREETING = "Hola ";
    private static final byte[] RESPONSE_GREETING_BYTES = {0x48, 0x6f, 0x6c, 0x61, 0x20};

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
    public void testWebsocketSingleClientForBytes() throws Exception {
        TestClient wsclient = new TestClient("ws://localhost:" + PORT + "/hola", 1);
        wsclient.connect();
        
        wsclient.sendBytesMessage("Cerveza".getBytes("UTF-8"));
        
        assertTrue(wsclient.await(10));
        assertEquals(1, wsclient.getReceivedBytes().size());
        assertEquals("Hola Cerveza", new String(wsclient.getReceivedBytes().get(0), "utf-8"));
        wsclient.close();
    }

    @Test
    public void testWebsocketSingleClientForReader() throws Exception {
        TestClient wsclient = new TestClient("ws://localhost:" + PORT + "/hola3", 1);
        wsclient.connect();
        
        wsclient.sendTextMessage("Cerveza");
        
        assertTrue(wsclient.await(10));
        assertEquals(1, wsclient.getReceived().size());
        assertEquals("Hola Cerveza", wsclient.getReceived().get(0));
        wsclient.close();
    }

    @Test
    public void testWebsocketSingleClientForInputStream() throws Exception {
        TestClient wsclient = new TestClient("ws://localhost:" + PORT + "/hola3", 1);
        wsclient.connect();
        
        wsclient.sendBytesMessage("Cerveza".getBytes("UTF-8"));
        
        assertTrue(wsclient.await(10));
        assertEquals(1, wsclient.getReceivedBytes().size());
        assertEquals("Hola Cerveza", new String(wsclient.getReceivedBytes().get(0), "utf-8"));
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
                // route for a single line
                from("wsservlet:///hola").to("log:info").process(new Processor() {
                    public void process(final Exchange exchange) throws Exception {
                        createResponse(exchange, false);
                    }
                }).to("wsservlet:///hola");

                // route for a broadcast line
                from("wsservlet:///hola2").to("log:info").process(new Processor() {
                    public void process(final Exchange exchange) throws Exception {
                        createResponse(exchange, false);
                    }
                }).to("wsservlet:///hola2?sendToAll=true");
                
                // route for a single stream line
                from("wsservlet:///hola3?useStreaming=true").to("log:info").process(new Processor() {
                    public void process(final Exchange exchange) throws Exception {
                        createResponse(exchange, true);
                    }
                }).to("wsservlet:///hola3");
                
            }
        };
    }

    private static void createResponse(Exchange exchange, boolean streaming) {
        Object msg = exchange.getIn().getBody();
        if (streaming) {
            assertTrue("Expects Reader or InputStream", msg instanceof Reader || msg instanceof InputStream);
        } else {
            assertTrue("Expects String or byte[]", msg instanceof String || msg instanceof byte[]);
        }
        
        if (msg instanceof String) {
            exchange.getIn().setBody(RESPONSE_GREETING + msg);     
        } else if (msg instanceof byte[]) {
            exchange.getIn().setBody(createByteResponse((byte[])msg));
        } else if (msg instanceof Reader) {
            exchange.getIn().setBody(new StringReader(RESPONSE_GREETING + readAll((Reader)msg)));
        } else if (msg instanceof InputStream) {
            exchange.getIn().setBody(createByteResponse(readAll((InputStream)msg)));
        }
    }
    
    private static byte[] createByteResponse(byte[] req) {
        byte[] resp = new byte[((byte[])req).length + RESPONSE_GREETING_BYTES.length];
        System.arraycopy(RESPONSE_GREETING_BYTES, 0, resp, 0, RESPONSE_GREETING_BYTES.length);
        System.arraycopy(req, 0, resp, RESPONSE_GREETING_BYTES.length, ((byte[])req).length);
        return resp;
    }

    private static String readAll(Reader reader) {
        StringBuffer strbuf = new StringBuffer();
        try {
            char[] buf = new char[4024];
            int n;
            while ((n = reader.read(buf, 0, buf.length)) > 0) {
                strbuf.append(buf, 0, n);
            }
        } catch (IOException e) {
            // ignore
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // ignore
            }
        }

        return strbuf.toString();
    }
    
    private static byte[] readAll(InputStream is) {
        ByteArrayOutputStream bytebuf = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[4024];
            int n;
            while ((n = is.read(buf, 0, buf.length)) > 0) {
                bytebuf.write(buf, 0, n);
            }
        } catch (IOException e) {
            // ignore
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // ignore
            }
        }

        return bytebuf.toByteArray();
    }
    // END SNIPPET: payload
}
