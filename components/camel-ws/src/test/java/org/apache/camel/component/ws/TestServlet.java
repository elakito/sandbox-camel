package org.apache.camel.component.ws;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

public class TestServlet extends WebSocketServlet {
    private static final long serialVersionUID = 1L;
    
    private List<String> messages;
    
    public TestServlet(List<String> messages) {
        this.messages = messages;
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        return new TestWsSocket();
    }

    private class TestWsSocket implements WebSocket.OnTextMessage {
        protected Connection con;

        @Override
        public void onOpen(Connection connection) {
            con = connection;
        }

        @Override
        public void onClose(int i, String s) {
        }

        @Override
        public void onMessage(String s) {
            try {
                messages.add(s);
                con.sendMessage(s);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
