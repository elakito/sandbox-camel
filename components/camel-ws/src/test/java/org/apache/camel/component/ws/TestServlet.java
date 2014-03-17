package org.apache.camel.component.ws;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

public class TestServlet extends WebSocketServlet {
    private static final long serialVersionUID = 1L;
    
    private List<Object> messages;
    
    public TestServlet(List<Object> messages) {
        this.messages = messages;
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        return new TestWsSocket();
    }

    private class TestWsSocket implements WebSocket.OnTextMessage, WebSocket.OnBinaryMessage {
        protected Connection con;

        @Override
        public void onOpen(Connection connection) {
            con = connection;
        }

        @Override
        public void onClose(int i, String s) {
        }

        @Override
        public void onMessage(String data) {
            try {
                messages.add(data);
                con.sendMessage(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onMessage(byte[] data, int offset, int length) {
            try {
                if (length < data.length) {
                    byte[] odata = data;
                    data = new byte[length];
                    System.arraycopy(odata, offset, data, 0, length);
                }
                messages.add(data);
                con.sendMessage(data, offset, length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
