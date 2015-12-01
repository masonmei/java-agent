/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.oped.apm.rpc.client;

import com.baidu.oped.apm.rpc.MessageListener;
import com.baidu.oped.apm.rpc.ApmSocket;
import com.baidu.oped.apm.rpc.packet.RequestPacket;
import com.baidu.oped.apm.rpc.packet.SendPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleMessageListener implements MessageListener {
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final SimpleMessageListener INSTANCE = new SimpleMessageListener();
    public static final SimpleMessageListener ECHO_INSTANCE = new SimpleMessageListener(true);

    private final boolean echo;

    public SimpleMessageListener() {
        this(false);
    }

    public SimpleMessageListener(boolean echo) {
        this.echo = echo;
    }

    @Override
    public void handleSend(SendPacket sendPacket, ApmSocket apmSocket) {
        logger.info("handleSend packet:{}, remote:{}", sendPacket, apmSocket.getRemoteAddress());
    }

    @Override
    public void handleRequest(RequestPacket requestPacket, ApmSocket apmSocket) {
        logger.info("handleRequest packet:{}, remote:{}", requestPacket, apmSocket.getRemoteAddress());

        if (echo) {
            apmSocket.response(requestPacket, requestPacket.getPayload());
        } else {
            apmSocket.response(requestPacket, new byte[0]);
        }
    }

}
