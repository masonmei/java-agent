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

import com.baidu.oped.apm.rpc.PinpointSocket;
import com.baidu.oped.apm.rpc.client.PinpointClient;
import com.baidu.oped.apm.rpc.client.PinpointClientFactory;
import com.baidu.oped.apm.rpc.packet.*;
import com.baidu.oped.apm.rpc.server.PinpointServer;
import com.baidu.oped.apm.rpc.server.PinpointServerAcceptor;
import com.baidu.oped.apm.rpc.server.ServerMessageListener;
import com.baidu.oped.apm.rpc.util.ClientFactoryUtils;
import com.baidu.oped.apm.thrift.dto.TApiMetaData;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * @author emeroad
 */
public class TcpDataSenderReconnectTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int PORT = 10050;
    public static final String HOST = "127.0.0.1";

    private int send;

    public PinpointServerAcceptor serverAcceptorStart() {
        PinpointServerAcceptor serverAcceptor = new PinpointServerAcceptor();
        serverAcceptor.setMessageListener(new ServerMessageListener() {

            @Override
            public void handleSend(SendPacket sendPacket, PinpointSocket pinpointSocket) {
                logger.info("handleSend packet:{}, remote:{}", sendPacket, pinpointSocket.getRemoteAddress());
                send++;
            }

            @Override
            public void handleRequest(RequestPacket requestPacket, PinpointSocket pinpointSocket) {
                logger.info("handleRequest packet:{}, remote:{}", requestPacket, pinpointSocket.getRemoteAddress());
            }

            @Override
            public HandshakeResponseCode handleHandshake(Map properties) {
                return HandshakeResponseType.Success.DUPLEX_COMMUNICATION;
            }

            @Override
            public void handlePing(PingPacket pingPacket, PinpointServer pinpointServer) {
                logger.info("ping received {} {} ", pingPacket, pinpointServer);
            }
        });
        serverAcceptor.bind(HOST, PORT);
        return serverAcceptor;
    }


    @Test
    public void connectAndSend() throws InterruptedException {
        PinpointServerAcceptor oldAcceptor = serverAcceptorStart();

        PinpointClientFactory clientFactory = createPinpointClientFactory();
        PinpointClient client = ClientFactoryUtils.createPinpointClient(HOST, PORT, clientFactory);

        TcpDataSender sender = new TcpDataSender(client);
        Thread.sleep(500);
        oldAcceptor.close();

        Thread.sleep(500);
        logger.info("Server start------------------");
        PinpointServerAcceptor serverAcceptor = serverAcceptorStart();

        Thread.sleep(5000);
        logger.info("sendMessage------------------");
        sender.send(new TApiMetaData("test", System.currentTimeMillis(), 1, "TestApi"));

        Thread.sleep(500);
        logger.info("sender stop------------------");
        sender.stop();

        serverAcceptor.close();
        client.close();
        clientFactory.release();
    }
    
    private PinpointClientFactory createPinpointClientFactory() {
        PinpointClientFactory clientFactory = new PinpointClientFactory();
        clientFactory.setTimeoutMillis(1000 * 5);
        clientFactory.setProperties(Collections.EMPTY_MAP);

        return clientFactory;
    }

}
