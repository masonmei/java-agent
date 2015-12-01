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

import com.baidu.oped.apm.rpc.ApmSocket;
import com.baidu.oped.apm.rpc.server.ApmServerAcceptor;
import com.baidu.oped.apm.rpc.server.SimpleServerMessageListener;
import com.baidu.oped.apm.rpc.util.ApmRPCTestUtils;
import com.baidu.oped.apm.rpc.util.ApmRPCTestUtils.EchoClientListener;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class ClientMessageListenerTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static int bindPort;

    @BeforeClass
    public static void setUp() throws IOException {
        bindPort = ApmRPCTestUtils.findAvailablePort();
    }

    @Test
    public void clientMessageListenerTest1() throws InterruptedException {
        ApmServerAcceptor serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort, SimpleServerMessageListener.DUPLEX_INSTANCE);

        EchoClientListener echoMessageListener = new EchoClientListener();
        ApmClientFactory clientSocketFactory = ApmRPCTestUtils.createClientFactory(ApmRPCTestUtils.getParams(), echoMessageListener);

        try {
            ApmClient client = clientSocketFactory.connect("127.0.0.1", bindPort);
            Thread.sleep(500);

            List<ApmSocket> writableServerList = serverAcceptor.getWritableSocketList();
            if (writableServerList.size() != 1) {
                Assert.fail();
            }

            ApmSocket writableServer = writableServerList.get(0);
            assertSendMessage(writableServer, "simple", echoMessageListener);
            assertRequestMessage(writableServer, "request", echoMessageListener);

            ApmRPCTestUtils.close(client);
        } finally {
            clientSocketFactory.release();
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void clientMessageListenerTest2() throws InterruptedException {
        ApmServerAcceptor serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort, SimpleServerMessageListener.DUPLEX_INSTANCE);

        EchoClientListener echoMessageListener1 = ApmRPCTestUtils.createEchoClientListener();
        ApmClientFactory clientSocketFactory1 = ApmRPCTestUtils.createClientFactory(ApmRPCTestUtils.getParams(), echoMessageListener1);

        EchoClientListener echoMessageListener2 = ApmRPCTestUtils.createEchoClientListener();
        ApmClientFactory clientSocketFactory2 = ApmRPCTestUtils.createClientFactory(ApmRPCTestUtils.getParams(), echoMessageListener2);

        try {
            ApmClient client = clientSocketFactory1.connect("127.0.0.1", bindPort);
            ApmClient client2 = clientSocketFactory2.connect("127.0.0.1", bindPort);

            Thread.sleep(500);

            List<ApmSocket> writableServerList = serverAcceptor.getWritableSocketList();
            if (writableServerList.size() != 2) {
                Assert.fail();
            }

            ApmSocket writableServer = writableServerList.get(0);
            assertRequestMessage(writableServer, "socket1", null);

            ApmSocket writableServer2 = writableServerList.get(1);
            assertRequestMessage(writableServer2, "socket2", null);

            Assert.assertEquals(1, echoMessageListener1.getRequestPacketRepository().size());
            Assert.assertEquals(1, echoMessageListener2.getRequestPacketRepository().size());

            ApmRPCTestUtils.close(client, client2);
        } finally {
            clientSocketFactory1.release();
            clientSocketFactory2.release();

            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    private void assertSendMessage(ApmSocket writableServer, String message, EchoClientListener echoMessageListener) throws InterruptedException {
        writableServer.send(message.getBytes());
        Thread.sleep(100);

        Assert.assertEquals(message, new String(echoMessageListener.getSendPacketRepository().get(0).getPayload()));
    }

    private void assertRequestMessage(ApmSocket writableServer, String message, EchoClientListener echoMessageListener) throws InterruptedException {
        byte[] response = ApmRPCTestUtils.request(writableServer, message.getBytes());
        Assert.assertEquals(message, new String(response));

        if (echoMessageListener != null) {
            Assert.assertEquals(message, new String(echoMessageListener.getRequestPacketRepository().get(0).getPayload()));
        }
    }

}
