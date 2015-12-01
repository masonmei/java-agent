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
import com.baidu.oped.apm.rpc.common.SocketStateCode;
import com.baidu.oped.apm.rpc.server.DefaultApmServer;
import com.baidu.oped.apm.rpc.server.ApmServerAcceptor;
import com.baidu.oped.apm.rpc.util.ApmRPCTestUtils;
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
public class ApmClientStateTest {

    private static int bindPort;

    @BeforeClass
    public static void setUp() throws IOException {
        bindPort = ApmRPCTestUtils.findAvailablePort();
    }

    @Test
    public void connectFailedStateTest() throws InterruptedException {
        ApmClientFactory clientFactory = null;
        DefaultApmClientHandler handler = null;
        try {
            clientFactory = ApmRPCTestUtils.createClientFactory(ApmRPCTestUtils.getParams(), ApmRPCTestUtils.createEchoClientListener());
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
        ApmServerAcceptor serverAcceptor = null;
        ApmClientFactory clientSocketFactory = null;
        DefaultApmClientHandler handler = null;
        try {
            serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort, ApmRPCTestUtils.createEchoServerListener());

            clientSocketFactory = ApmRPCTestUtils.createClientFactory(ApmRPCTestUtils.getParams(), ApmRPCTestUtils.createEchoClientListener());
            handler = connect(clientSocketFactory);
            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.RUN_DUPLEX, handler.getCurrentStateCode());
            handler.close();

            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.CLOSED_BY_CLIENT, handler.getCurrentStateCode());
        } finally {
            closeHandler(handler);
            closeSocketFactory(clientSocketFactory);
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void closeByPeerStateTest() throws InterruptedException {
        ApmServerAcceptor serverAcceptor = null;
        ApmClientFactory clientFactory = null;
        DefaultApmClientHandler handler = null;
        try {
            serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort, ApmRPCTestUtils.createEchoServerListener());

            clientFactory = ApmRPCTestUtils.createClientFactory(ApmRPCTestUtils.getParams(), ApmRPCTestUtils.createEchoClientListener());
            handler = connect(clientFactory);
            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.RUN_DUPLEX, handler.getCurrentStateCode());
            serverAcceptor.close();

            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.CLOSED_BY_SERVER, handler.getCurrentStateCode());
        } finally {
            closeHandler(handler);
            closeSocketFactory(clientFactory);
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void unexpectedCloseStateTest() throws InterruptedException {
        ApmServerAcceptor serverAcceptor = null;
        ApmClientFactory clientFactory = null;
        DefaultApmClientHandler handler = null;
        try {
            serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort, ApmRPCTestUtils.createEchoServerListener());

            clientFactory = ApmRPCTestUtils.createClientFactory(ApmRPCTestUtils.getParams(), ApmRPCTestUtils.createEchoClientListener());
            handler = connect(clientFactory);
            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.RUN_DUPLEX, handler.getCurrentStateCode());
            clientFactory.release();

            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.UNEXPECTED_CLOSE_BY_CLIENT, handler.getCurrentStateCode());
        } finally {
            closeHandler(handler);
            closeSocketFactory(clientFactory);
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void unexpectedCloseByPeerStateTest() throws InterruptedException {
        ApmServerAcceptor serverAcceptor = null;
        ApmClientFactory clientFactory = null;
        DefaultApmClientHandler handler = null;
        try {
            serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort, ApmRPCTestUtils.createEchoServerListener());

            clientFactory = ApmRPCTestUtils.createClientFactory(ApmRPCTestUtils.getParams(), ApmRPCTestUtils.createEchoClientListener());
            handler = connect(clientFactory);
            Thread.sleep(1000);

            List<ApmSocket> apmServerList = serverAcceptor.getWritableSocketList();
            ApmSocket apmServer = apmServerList.get(0);
            Assert.assertEquals(SocketStateCode.RUN_DUPLEX, handler.getCurrentStateCode());

            ((DefaultApmServer) apmServer).stop(true);

            Thread.sleep(1000);

            Assert.assertEquals(SocketStateCode.UNEXPECTED_CLOSE_BY_SERVER, handler.getCurrentStateCode());
        } finally {
            closeHandler(handler);
            closeSocketFactory(clientFactory);
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    private DefaultApmClientHandler connect(ApmClientFactory factory) {
        ChannelFuture future = factory.reconnect(new InetSocketAddress("127.0.0.1", bindPort));
        ApmClientHandler handler = getSocketHandler(future, new InetSocketAddress("127.0.0.1", bindPort));
        return (DefaultApmClientHandler) handler;
    }

    ApmClientHandler getSocketHandler(ChannelFuture channelConnectFuture, SocketAddress address) {
        if (address == null) {
            throw new NullPointerException("address");
        }

        Channel channel = channelConnectFuture.getChannel();
        ApmClientHandler apmClientHandler = (ApmClientHandler) channel.getPipeline().getLast();
        apmClientHandler.setConnectSocketAddress(address);

        return apmClientHandler;
    }

    private void closeHandler(DefaultApmClientHandler handler) {
        if (handler != null) {
            handler.close();
        }
    }

    private void closeSocketFactory(ApmClientFactory factory) {
        if (factory != null) {
            factory.release();
        }
    }

}
