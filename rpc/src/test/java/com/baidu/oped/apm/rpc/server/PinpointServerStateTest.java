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

package com.baidu.oped.apm.rpc.server;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;

import com.baidu.oped.apm.rpc.PinpointSocket;
import com.baidu.oped.apm.rpc.client.PinpointClient;
import com.baidu.oped.apm.rpc.client.PinpointClientFactory;
import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.oped.apm.rpc.common.SocketStateCode;
import com.baidu.oped.apm.rpc.control.ProtocolException;
import com.baidu.oped.apm.rpc.packet.ControlHandshakePacket;
import com.baidu.oped.apm.rpc.util.ControlMessageEncodingUtils;
import com.baidu.oped.apm.rpc.util.PinpointRPCTestUtils;

/**
 * @author Taejin Koo
 */
public class PinpointServerStateTest {

    private static int bindPort;

    @BeforeClass
    public static void setUp() throws IOException {
        bindPort = PinpointRPCTestUtils.findAvailablePort();
    }

    @Test
    public void closeByPeerTest() throws InterruptedException {
        PinpointServerAcceptor serverAcceptor = null;
        PinpointClient client = null;
        PinpointClientFactory clientFactory = null;
        try {
            serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort, PinpointRPCTestUtils.createEchoServerListener());

            clientFactory = PinpointRPCTestUtils.createClientFactory(PinpointRPCTestUtils.getParams(), PinpointRPCTestUtils.createEchoClientListener());
            client = clientFactory.connect("127.0.0.1", bindPort);
            Thread.sleep(1000);

            List<PinpointSocket> pinpointServerList = serverAcceptor.getWritableSocketList();
            PinpointSocket pinpointServer = pinpointServerList.get(0);

            if (pinpointServer instanceof  PinpointServer) {
                Assert.assertEquals(SocketStateCode.RUN_DUPLEX, ((PinpointServer) pinpointServer).getCurrentStateCode());

                client.close();
                Thread.sleep(1000);

                Assert.assertEquals(SocketStateCode.CLOSED_BY_CLIENT, ((PinpointServer)pinpointServer).getCurrentStateCode());
            } else {
                Assert.fail();
            }

        } finally {
            PinpointRPCTestUtils.close(client);
            if (clientFactory != null) {
                clientFactory.release();
            }
            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void closeTest() throws InterruptedException {
        PinpointServerAcceptor serverAcceptor = null;
        PinpointClient client = null;
        PinpointClientFactory clientFactory = null;
        try {
            serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort, PinpointRPCTestUtils.createEchoServerListener());

            clientFactory = PinpointRPCTestUtils.createClientFactory(PinpointRPCTestUtils.getParams(), PinpointRPCTestUtils.createEchoClientListener());
            client = clientFactory.connect("127.0.0.1", bindPort);
            Thread.sleep(1000);

            List<PinpointSocket> pinpointServerList = serverAcceptor.getWritableSocketList();
            PinpointSocket pinpointServer = pinpointServerList.get(0);
            Assert.assertEquals(SocketStateCode.RUN_DUPLEX, ((PinpointServer) pinpointServer).getCurrentStateCode());

            serverAcceptor.close();
            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.CLOSED_BY_SERVER, ((PinpointServer)pinpointServer).getCurrentStateCode());
        } finally {
            PinpointRPCTestUtils.close(client);
            if (clientFactory != null) {
                clientFactory.release();
            }
            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void unexpectedCloseByPeerTest() throws InterruptedException, IOException, ProtocolException {
        PinpointServerAcceptor serverAcceptor = null;
        try {
            serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort, PinpointRPCTestUtils.createEchoServerListener());

            Socket socket = new Socket("127.0.0.1", bindPort);
            socket.getOutputStream().write(createHandshakePayload(PinpointRPCTestUtils.getParams()));
            socket.getOutputStream().flush();
            Thread.sleep(1000);

            List<PinpointSocket> pinpointServerList = serverAcceptor.getWritableSocketList();
            PinpointSocket pinpointServer = pinpointServerList.get(0);
            if (!(pinpointServer instanceof  PinpointServer)) {
                socket.close();
                Assert.fail();
            }

            Assert.assertEquals(SocketStateCode.RUN_DUPLEX, ((PinpointServer)pinpointServer).getCurrentStateCode());

            socket.close();
            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.UNEXPECTED_CLOSE_BY_CLIENT, ((PinpointServer)pinpointServer).getCurrentStateCode());
        } finally {
            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void unexpectedCloseTest() throws InterruptedException, IOException, ProtocolException {
        PinpointServerAcceptor serverAcceptor = null;
        PinpointClient client = null;
        PinpointClientFactory clientFactory = null;
        try {
            serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort, PinpointRPCTestUtils.createEchoServerListener());

            clientFactory = PinpointRPCTestUtils.createClientFactory(PinpointRPCTestUtils.getParams(), PinpointRPCTestUtils.createEchoClientListener());
            client = clientFactory.connect("127.0.0.1", bindPort);
            Thread.sleep(1000);

            List<PinpointSocket> pinpointServerList = serverAcceptor.getWritableSocketList();
            PinpointSocket pinpointServer = pinpointServerList.get(0);

            Assert.assertEquals(SocketStateCode.RUN_DUPLEX, ((PinpointServer) pinpointServer).getCurrentStateCode());

            ((DefaultPinpointServer)pinpointServer).stop(true);
            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.UNEXPECTED_CLOSE_BY_SERVER, ((PinpointServer)pinpointServer).getCurrentStateCode());
        } finally {
            PinpointRPCTestUtils.close(client);
            if (clientFactory != null) {
                clientFactory.release();
            }
            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }
    
    private byte[] createHandshakePayload(Map<String, Object> data) throws ProtocolException {
        byte[] payload = ControlMessageEncodingUtils.encode(data);
        ControlHandshakePacket handshakePacket = new ControlHandshakePacket(payload);
        ChannelBuffer channelBuffer = handshakePacket.toBuffer();
        return channelBuffer.toByteBuffer().array();
    }

}
