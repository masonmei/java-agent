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

import com.baidu.oped.apm.rpc.PinpointSocket;
import com.baidu.oped.apm.rpc.server.PinpointServerAcceptor;
import com.baidu.oped.apm.rpc.server.SimpleServerMessageListener;
import com.baidu.oped.apm.rpc.util.PinpointRPCTestUtils;
import com.baidu.oped.apm.rpc.util.PinpointRPCTestUtils.EchoClientListener;
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
        bindPort = PinpointRPCTestUtils.findAvailablePort();
    }

    @Test
    public void clientMessageListenerTest1() throws InterruptedException {
        PinpointServerAcceptor serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort, SimpleServerMessageListener.DUPLEX_INSTANCE);

        EchoClientListener echoMessageListener = new EchoClientListener();
        PinpointClientFactory clientSocketFactory = PinpointRPCTestUtils.createClientFactory(PinpointRPCTestUtils.getParams(), echoMessageListener);

        try {
            PinpointClient client = clientSocketFactory.connect("127.0.0.1", bindPort);
            Thread.sleep(500);

            List<PinpointSocket> writableServerList = serverAcceptor.getWritableSocketList();
            if (writableServerList.size() != 1) {
                Assert.fail();
            }

            PinpointSocket writableServer = writableServerList.get(0);
            assertSendMessage(writableServer, "simple", echoMessageListener);
            assertRequestMessage(writableServer, "request", echoMessageListener);

            PinpointRPCTestUtils.close(client);
        } finally {
            clientSocketFactory.release();
            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void clientMessageListenerTest2() throws InterruptedException {
        PinpointServerAcceptor serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort, SimpleServerMessageListener.DUPLEX_INSTANCE);

        EchoClientListener echoMessageListener1 = PinpointRPCTestUtils.createEchoClientListener();
        PinpointClientFactory clientSocketFactory1 = PinpointRPCTestUtils.createClientFactory(PinpointRPCTestUtils.getParams(), echoMessageListener1);

        EchoClientListener echoMessageListener2 = PinpointRPCTestUtils.createEchoClientListener();
        PinpointClientFactory clientSocketFactory2 = PinpointRPCTestUtils.createClientFactory(PinpointRPCTestUtils.getParams(), echoMessageListener2);

        try {
            PinpointClient client = clientSocketFactory1.connect("127.0.0.1", bindPort);
            PinpointClient client2 = clientSocketFactory2.connect("127.0.0.1", bindPort);

            Thread.sleep(500);

            List<PinpointSocket> writableServerList = serverAcceptor.getWritableSocketList();
            if (writableServerList.size() != 2) {
                Assert.fail();
            }

            PinpointSocket writableServer = writableServerList.get(0);
            assertRequestMessage(writableServer, "socket1", null);

            PinpointSocket writableServer2 = writableServerList.get(1);
            assertRequestMessage(writableServer2, "socket2", null);

            Assert.assertEquals(1, echoMessageListener1.getRequestPacketRepository().size());
            Assert.assertEquals(1, echoMessageListener2.getRequestPacketRepository().size());

            PinpointRPCTestUtils.close(client, client2);
        } finally {
            clientSocketFactory1.release();
            clientSocketFactory2.release();

            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }

    private void assertSendMessage(PinpointSocket writableServer, String message, EchoClientListener echoMessageListener) throws InterruptedException {
        writableServer.send(message.getBytes());
        Thread.sleep(100);

        Assert.assertEquals(message, new String(echoMessageListener.getSendPacketRepository().get(0).getPayload()));
    }

    private void assertRequestMessage(PinpointSocket writableServer, String message, EchoClientListener echoMessageListener) throws InterruptedException {
        byte[] response = PinpointRPCTestUtils.request(writableServer, message.getBytes());
        Assert.assertEquals(message, new String(response));

        if (echoMessageListener != null) {
            Assert.assertEquals(message, new String(echoMessageListener.getRequestPacketRepository().get(0).getPayload()));
        }
    }

}
