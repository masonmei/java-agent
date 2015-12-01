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

package com.baidu.oped.apm.profiler.sender;

import com.baidu.oped.apm.rpc.ApmSocket;
import com.baidu.oped.apm.rpc.client.ApmClient;
import com.baidu.oped.apm.rpc.client.ApmClientFactory;
import com.baidu.oped.apm.rpc.packet.*;
import com.baidu.oped.apm.rpc.server.ApmServer;
import com.baidu.oped.apm.rpc.server.ApmServerAcceptor;
import com.baidu.oped.apm.rpc.server.ServerMessageListener;
import com.baidu.oped.apm.rpc.util.ClientFactoryUtils;
import com.baidu.oped.apm.thrift.dto.TApiMetaData;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author emeroad
 */
public class TcpDataSenderTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int PORT = 10050;
    public static final String HOST = "127.0.0.1";

    private ApmServerAcceptor serverAcceptor;
    private CountDownLatch sendLatch;

    @Before
    public void serverStart() {
        serverAcceptor = new ApmServerAcceptor();
        serverAcceptor.setMessageListener(new ServerMessageListener() {

            @Override
            public void handleSend(SendPacket sendPacket, ApmSocket apmSocket) {
                logger.info("handleSend packet:{}, remote:{}", sendPacket, apmSocket.getRemoteAddress());
                if (sendLatch != null) {
                    sendLatch.countDown();
                }
            }

            @Override
            public void handleRequest(RequestPacket requestPacket, ApmSocket apmSocket) {
                logger.info("handleRequest packet:{}, remote:{}", requestPacket, apmSocket.getRemoteAddress());
            }

            @Override
            public HandshakeResponseCode handleHandshake(Map arg0) {
                return HandshakeResponseType.Success.DUPLEX_COMMUNICATION;
            }

            @Override
            public void handlePing(PingPacket pingPacket, ApmServer apmServer) {
                logger.info("ping received {} {} ", pingPacket, apmServer);
            }
        });
        serverAcceptor.bind(HOST, PORT);
    }

    @After
    public void serverShutdown() {
        if (serverAcceptor != null) {
            serverAcceptor.close();
        }
    }

    @Test
    public void connectAndSend() throws InterruptedException {
        this.sendLatch = new CountDownLatch(2);

        ApmClientFactory clientFactory = createApmClientFactory();
        
        ApmClient client = ClientFactoryUtils.createApmClient(HOST, PORT, clientFactory);
        
        TcpDataSender sender = new TcpDataSender(client);
        try {
            sender.send(new TApiMetaData("test", System.currentTimeMillis(), 1, "TestApi"));
            sender.send(new TApiMetaData("test", System.currentTimeMillis(), 1, "TestApi"));


            boolean received = sendLatch.await(1000, TimeUnit.MILLISECONDS);
            Assert.assertTrue(received);
        } finally {
            sender.stop();
            
            if (client != null) {
                client.close();
            }
            
            if (clientFactory != null) {
                clientFactory.release();
            }
        }
    }
    
    private ApmClientFactory createApmClientFactory() {
        ApmClientFactory clientFactory = new ApmClientFactory();
        clientFactory.setTimeoutMillis(1000 * 5);
        clientFactory.setProperties(Collections.EMPTY_MAP);

        return clientFactory;
    }

}
