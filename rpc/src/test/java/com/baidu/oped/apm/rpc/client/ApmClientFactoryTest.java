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

import com.baidu.oped.apm.rpc.ApmSocketException;
import com.baidu.oped.apm.rpc.TestByteUtils;
import com.baidu.oped.apm.rpc.server.ApmServerAcceptor;
import com.baidu.oped.apm.rpc.server.SimpleServerMessageListener;
import com.baidu.oped.apm.rpc.util.ApmRPCTestUtils;
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
public class ApmClientFactoryTest {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static int bindPort;
    private static ApmClientFactory clientFactory;
    
    @BeforeClass
    public static void setUp() throws IOException {
        bindPort = ApmRPCTestUtils.findAvailablePort();

        clientFactory = new ApmClientFactory();
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
        } catch (ApmSocketException e) {
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
        ApmServerAcceptor serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort);

        try {
            ApmClient client = clientFactory.connect("127.0.0.1", bindPort);
            ApmRPCTestUtils.close(client);
        } finally {
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void pingInternal() throws IOException, InterruptedException {
        ApmServerAcceptor serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort);

        try {
            ApmClient client = clientFactory.connect("127.0.0.1", bindPort);
            Thread.sleep(1000);
            ApmRPCTestUtils.close(client);
        } finally {
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void ping() throws IOException, InterruptedException {
        ApmServerAcceptor serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort);

        try {
            ApmClient client = clientFactory.connect("127.0.0.1", bindPort);
            client.sendPing();
            ApmRPCTestUtils.close(client);
        } finally {
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void pingAndRequestResponse() throws IOException, InterruptedException {
        ApmServerAcceptor serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort, SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE);

        try {
            ApmClient client = clientFactory.connect("127.0.0.1", bindPort);
            
            byte[] randomByte = TestByteUtils.createRandomByte(10);
            byte[] response = ApmRPCTestUtils.request(client, randomByte);
            
            Assert.assertArrayEquals(randomByte, response);
            ApmRPCTestUtils.close(client);
        } finally {
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void sendSync() throws IOException, InterruptedException {
        ApmServerAcceptor serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort, SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE);

        try {
            ApmClient client = clientFactory.connect("127.0.0.1", bindPort);
            logger.info("send1");
            client.send(new byte[20]);
            logger.info("send2");
            client.sendSync(new byte[20]);

            ApmRPCTestUtils.close(client);
        } finally {
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void requestAndResponse() throws IOException, InterruptedException {
        ApmServerAcceptor serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort, SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE);

        try {
            ApmClient client = clientFactory.connect("127.0.0.1", bindPort);

            byte[] randomByte = TestByteUtils.createRandomByte(20);
            byte[] response = ApmRPCTestUtils.request(client, randomByte);

            Assert.assertArrayEquals(randomByte, response);
            ApmRPCTestUtils.close(client);
        } finally {
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void connectTimeout() {
        int timeout = 1000;

        ApmClientFactory apmClientFactory = null;
        try {
            apmClientFactory = new ApmClientFactory();
            apmClientFactory.setConnectTimeout(timeout);
            int connectTimeout = apmClientFactory.getConnectTimeout();
            
            Assert.assertEquals(timeout, connectTimeout);
        } finally {
            apmClientFactory.release();
        }
    }
    
}
