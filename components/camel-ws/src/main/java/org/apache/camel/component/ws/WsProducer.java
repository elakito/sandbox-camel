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

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;

/**
 *
 */
public class WsProducer extends DefaultProducer {
    private final WsEndpoint endpoint;

    public WsProducer(WsEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public WsEndpoint getEndpoint() {
        return (WsEndpoint) super.getEndpoint();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        Object message = in.getBody();
        log.debug("Sending out {}", message);
        if (message != null) {
            if (message instanceof String) {
                endpoint.getWebSocket().sendTextMessage((String)message);
            } else if (message instanceof byte[]) {
                endpoint.getWebSocket().sendMessage((byte[])message);
            } else {
                //TODO provide other binding option, for now use the converted string
                endpoint.getWebSocket().sendTextMessage(in.getMandatoryBody(String.class));
            }
        }
    }
}
