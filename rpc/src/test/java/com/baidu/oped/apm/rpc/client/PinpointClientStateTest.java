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
import com.baidu.oped.apm.rpc.common.SocketStateCode;
import com.baidu.oped.apm.rpc.server.DefaultPinpointServer;
import com.baidu.oped.apm.rpc.server.PinpointServerAcceptor;
import com.baidu.oped.apm.rpc.util.PinpointRPCTestUtils;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

/**
 * @author Taejin Koo
 */
public class PinpointClientStateTest {

    private static int bindPort;

    @BeforeClass
    public static void setUp() throws IOException {
        bindPort = PinpointRPCTestUtils.findAvailablePort();
    }

    @Test
    public void connectFailedStateTest() throws InterruptedException {
        PinpointClientFactory clientFactory = null;
        DefaultPinpointClientHandler handler = null;
        try {
            clientFactory = PinpointRPCTestUtils.createClientFactory(PinpointRPCTestUtils.getParams(), PinpointRPCTestUtils.createEchoClientListener());
            handler = connect(clientFactory);

            Thread.sleep(2000);

            Assert.assertEquals(SocketStateCode.CONNECT_FAILED, handler.getCurrentStateCode());
        } finally {
            closeHandler(handler);
            closeSocketFactory(clientFactory);
        }
    }

    @Test
    public void closeStateTest() throws InterruptedException {
        PinpointServerAcceptor serverAcceptor = null;
        PinpointClientFactory clientSocketFactory = null;
        DefaultPinpointClientHandler handler = null;
        try {
            serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort, PinpointRPCTestUtils.createEchoServerListener());

            clientSocketFactory = PinpointRPCTestUtils.createClientFactory(PinpointRPCTestUtils.getParams(), PinpointRPCTestUtils.createEchoClientListener());
            handler = connect(clientSocketFactory);
            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.RUN_DUPLEX, handler.getCurrentStateCode());
            handler.close();

            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.CLOSED_BY_CLIENT, handler.getCurrentStateCode());
        } finally {
            closeHandler(handler);
            closeSocketFactory(clientSocketFactory);
            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void closeByPeerStateTest() throws InterruptedException {
        PinpointServerAcceptor serverAcceptor = null;
        PinpointClientFactory clientFactory = null;
        DefaultPinpointClientHandler handler = null;
        try {
            serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort, PinpointRPCTestUtils.createEchoServerListener());

            clientFactory = PinpointRPCTestUtils.createClientFactory(PinpointRPCTestUtils.getParams(), PinpointRPCTestUtils.createEchoClientListener());
            handler = connect(clientFactory);
            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.RUN_DUPLEX, handler.getCurrentStateCode());
            serverAcceptor.close();

            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.CLOSED_BY_SERVER, handler.getCurrentStateCode());
        } finally {
            closeHandler(handler);
            closeSocketFactory(clientFactory);
            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void unexpectedCloseStateTest() throws InterruptedException {
        PinpointServerAcceptor serverAcceptor = null;
        PinpointClientFactory clientFactory = null;
        DefaultPinpointClientHandler handler = null;
        try {
            serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort, PinpointRPCTestUtils.createEchoServerListener());

            clientFactory = PinpointRPCTestUtils.createClientFactory(PinpointRPCTestUtils.getParams(), PinpointRPCTestUtils.createEchoClientListener());
            handler = connect(clientFactory);
            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.RUN_DUPLEX, handler.getCurrentStateCode());
            clientFactory.release();

            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.UNEXPECTED_CLOSE_BY_CLIENT, handler.getCurrentStateCode());
        } finally {
            closeHandler(handler);
            closeSocketFactory(clientFactory);
            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void unexpectedCloseByPeerStateTest() throws InterruptedException {
        PinpointServerAcceptor serverAcceptor = null;
        PinpointClientFactory clientFactory = null;
        DefaultPinpointClientHandler handler = null;
        try {
            serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort, PinpointRPCTestUtils.createEchoServerListener());

            clientFactory = PinpointRPCTestUtils.createClientFactory(PinpointRPCTestUtils.getParams(), PinpointRPCTestUtils.createEchoClientListener());
            handler = connect(clientFactory);
            Thread.sleep(1000);

            List<PinpointSocket> pinpointServerList = serverAcceptor.getWritableSocketList();
            PinpointSocket pinpointServer = pinpointServerList.get(0);
            Assert.assertEquals(SocketStateCode.RUN_DUPLEX, handler.getCurrentStateCode());

            ((DefaultPinpointServer) pinpointServer).stop(true);

            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.UNEXPECTED_CLOSE_BY_SERVER, handler.getCurrentStateCode());
        } finally {
            closeHandler(handler);
            closeSocketFactory(clientFactory);
            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }

    private DefaultPinpointClientHandler connect(PinpointClientFactory factory) {
        ChannelFuture future = factory.reconnect(new InetSocketAddress("127.0.0.1", bindPort));
        PinpointClientHandler handler = getSocketHandler(future, new InetSocketAddress("127.0.0.1", bindPort));
        return (DefaultPinpointClientHandler) handler;
    }

    PinpointClientHandler getSocketHandler(ChannelFuture channelConnectFuture, SocketAddress address) {
        if (address == null) {
            throw new NullPointerException("address");
        }

        Channel channel = channelConnectFuture.getChannel();
        PinpointClientHandler pinpointClientHandler = (PinpointClientHandler) channel.getPipeline().getLast();
        pinpointClientHandler.setConnectSocketAddress(address);

        return pinpointClientHandler;
    }

    private void closeHandler(DefaultPinpointClientHandler handler) {
        if (handler != null) {
            handler.close();
        }
    }

    private void closeSocketFactory(PinpointClientFactory factory) {
        if (factory != null) {
            factory.release();
        }
    }

}
