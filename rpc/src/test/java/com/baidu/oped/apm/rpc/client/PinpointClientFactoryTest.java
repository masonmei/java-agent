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

import com.baidu.oped.apm.rpc.PinpointSocketException;
import com.baidu.oped.apm.rpc.TestByteUtils;
import com.baidu.oped.apm.rpc.server.PinpointServerAcceptor;
import com.baidu.oped.apm.rpc.server.SimpleServerMessageListener;
import com.baidu.oped.apm.rpc.util.PinpointRPCTestUtils;
import org.jboss.netty.channel.ChannelFuture;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;


/**
 * @author emeroad
 */
public class PinpointClientFactoryTest {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static int bindPort;
    private static PinpointClientFactory clientFactory;
    
    @BeforeClass
    public static void setUp() throws IOException {
        bindPort = PinpointRPCTestUtils.findAvailablePort();

        clientFactory = new PinpointClientFactory();
        clientFactory.setPingDelay(100);
    }
    
    @AfterClass
    public static void tearDown() {
        if (clientFactory != null) {
            clientFactory.release();
        }
    }

    @Test
    public void connectFail() {
        try {
            clientFactory.connect("127.0.0.1", bindPort);
            Assert.fail();
        } catch (PinpointSocketException e) {
            Assert.assertTrue(ConnectException.class.isInstance(e.getCause()));
        } 
    }

    @Test
    public void reconnectFail() throws InterruptedException {
        // confirm simplified error message when api called.
        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", bindPort);
        ChannelFuture reconnect = clientFactory.reconnect(remoteAddress);
        reconnect.await();
        Assert.assertFalse(reconnect.isSuccess());
        Assert.assertTrue(ConnectException.class.isInstance(reconnect.getCause()));
        
        Thread.sleep(1000);
    }

    @Test
    public void connect() throws IOException, InterruptedException {
        PinpointServerAcceptor serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort);

        try {
            PinpointClient client = clientFactory.connect("127.0.0.1", bindPort);
            PinpointRPCTestUtils.close(client);
        } finally {
            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void pingInternal() throws IOException, InterruptedException {
        PinpointServerAcceptor serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort);

        try {
            PinpointClient client = clientFactory.connect("127.0.0.1", bindPort);
            Thread.sleep(1000);
            PinpointRPCTestUtils.close(client);
        } finally {
            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void ping() throws IOException, InterruptedException {
        PinpointServerAcceptor serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort);

        try {
            PinpointClient client = clientFactory.connect("127.0.0.1", bindPort);
            client.sendPing();
            PinpointRPCTestUtils.close(client);
        } finally {
            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void pingAndRequestResponse() throws IOException, InterruptedException {
        PinpointServerAcceptor serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort, SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE);

        try {
            PinpointClient client = clientFactory.connect("127.0.0.1", bindPort);
            
            byte[] randomByte = TestByteUtils.createRandomByte(10);
            byte[] response = PinpointRPCTestUtils.request(client, randomByte);
            
            Assert.assertArrayEquals(randomByte, response);
            PinpointRPCTestUtils.close(client);
        } finally {
            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void sendSync() throws IOException, InterruptedException {
        PinpointServerAcceptor serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort, SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE);

        try {
            PinpointClient client = clientFactory.connect("127.0.0.1", bindPort);
            logger.info("send1");
            client.send(new byte[20]);
            logger.info("send2");
            client.sendSync(new byte[20]);

            PinpointRPCTestUtils.close(client);
        } finally {
            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void requestAndResponse() throws IOException, InterruptedException {
        PinpointServerAcceptor serverAcceptor = PinpointRPCTestUtils.createPinpointServerFactory(bindPort, SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE);

        try {
            PinpointClient client = clientFactory.connect("127.0.0.1", bindPort);

            byte[] randomByte = TestByteUtils.createRandomByte(20);
            byte[] response = PinpointRPCTestUtils.request(client, randomByte);

            Assert.assertArrayEquals(randomByte, response);
            PinpointRPCTestUtils.close(client);
        } finally {
            PinpointRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void connectTimeout() {
        int timeout = 1000;

        PinpointClientFactory pinpointClientFactory = null;
        try {
            pinpointClientFactory = new PinpointClientFactory();
            pinpointClientFactory.setConnectTimeout(timeout);
            int connectTimeout = pinpointClientFactory.getConnectTimeout();
            
            Assert.assertEquals(timeout, connectTimeout);
        } finally {
            pinpointClientFactory.release();
        }
    }
    
}
