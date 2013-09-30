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

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;

public class WsConsumer extends DefaultConsumer {

    public WsConsumer(WsEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    @Override
    public void start() throws Exception {
        super.start();
        getEndpoint().connect(this);
    }

    @Override
    public void stop() throws Exception {
        getEndpoint().disconnect(this);
        super.stop();
    }

    @Override
    public WsEndpoint getEndpoint() {
        return (WsEndpoint) super.getEndpoint();
    }

    public void sendMessage(final String message) {

        final Exchange exchange = getEndpoint().createExchange();

        // set header and body
//        exchange.getIn().setHeader(WsConstants.CONNECTION_KEY, connectionKey);
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
