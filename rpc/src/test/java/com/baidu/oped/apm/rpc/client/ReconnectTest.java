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

import com.baidu.oped.apm.rpc.Future;
import com.baidu.oped.apm.rpc.ApmSocketException;
import com.baidu.oped.apm.rpc.ResponseMessage;
import com.baidu.oped.apm.rpc.TestByteUtils;
import com.baidu.oped.apm.rpc.server.ApmServerAcceptor;
import com.baidu.oped.apm.rpc.server.SimpleServerMessageListener;
import com.baidu.oped.apm.rpc.util.ApmRPCTestUtils;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author emeroad
 */
//@Ignore
public class ReconnectTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static int bindPort;
    private static ApmClientFactory clientFactory;
    
    @BeforeClass
    public static void setUp() throws IOException {
        bindPort = ApmRPCTestUtils.findAvailablePort();
        
        clientFactory = new ApmClientFactory();
        clientFactory.setReconnectDelay(200);
        clientFactory.setPingDelay(100);
        clientFactory.setTimeoutMillis(200);
    }
    
    @AfterClass
    public static void tearDown() {
        if (clientFactory != null) {
            clientFactory.release();
        }
    }


    @Test
    public void reconnect() throws IOException, InterruptedException {
        ApmServerAcceptor serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort, SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE);
        
        final AtomicBoolean reconnectPerformed = new AtomicBoolean(false);

        ApmServerAcceptor newServerAcceptor = null;
        try {
            ApmClient client = clientFactory.connect("localhost", bindPort);
            client.addApmClientReconnectEventListener(new ApmClientReconnectEventListener() {

                @Override
                public void reconnectPerformed(ApmClient client) {
                    reconnectPerformed.set(true);
                }

            });
            
            ApmRPCTestUtils.close(serverAcceptor);

            logger.info("server.close()---------------------------");
            Thread.sleep(1000);
            try {
                byte[] response = ApmRPCTestUtils.request(client, new byte[10]);
                Assert.fail("expected:exception");
            } catch (Exception e) {
                // skip because of expected error
            }

            newServerAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort, SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE);
            logger.info("bind server---------------------------");

            Thread.sleep(3000);
            logger.info("request server---------------------------");
            byte[] randomByte = TestByteUtils.createRandomByte(10);
            byte[] response = ApmRPCTestUtils.request(client, randomByte);
            
            Assert.assertArrayEquals(randomByte, response);
            
            ApmRPCTestUtils.close(client);
        } finally {
            ApmRPCTestUtils.close(newServerAcceptor);
        }
        
        Assert.assertTrue(reconnectPerformed.get());
    }
    
    // it takes very long time. 
    // @Test
    @Ignore
    public void reconnectStressTest() throws IOException, InterruptedException {
        int count = 3;
        
        ThreadMXBean tbean = ManagementFactory.getThreadMXBean();

        int threadCount = tbean.getThreadCount();
        for (int i = 0; i < count; i++) {
            logger.info((i + 1) + "th's start.");
            
            ApmServerAcceptor serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort, SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE);
            ApmClient socket = clientFactory.connect("localhost", bindPort);
            ApmRPCTestUtils.close(serverAcceptor);

            logger.info("server.close()---------------------------");
            Thread.sleep(10000);

            serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort, SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE);
            logger.info("bind server---------------------------");

            Thread.sleep(10000);
            logger.info("request server---------------------------");
            byte[] randomByte = TestByteUtils.createRandomByte(10);
            byte[] response = ApmRPCTestUtils.request(socket, randomByte);

            Assert.assertArrayEquals(randomByte, response);

            ApmRPCTestUtils.close(socket);
            ApmRPCTestUtils.close(serverAcceptor);
        }
        
        Thread.sleep(10000);

        Assert.assertEquals(threadCount, tbean.getThreadCount());
    }


    @Test
    public void scheduledConnect() throws IOException, InterruptedException {
        final ApmClientFactory clientFactory = new ApmClientFactory();
        clientFactory.setReconnectDelay(200);
        ApmClient client = null;
        ApmServerAcceptor serverAcceptor = null;
        try {
            client = clientFactory.scheduledConnect("localhost", bindPort);

            serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort, SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE);

            Thread.sleep(2000);
            logger.info("request server---------------------------");
            byte[] randomByte = TestByteUtils.createRandomByte(10);
            byte[] response = ApmRPCTestUtils.request(client, randomByte);

            Assert.assertArrayEquals(randomByte, response);
        } finally {
            ApmRPCTestUtils.close(client);
            clientFactory.release();
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void scheduledConnectAndClosed() throws IOException, InterruptedException {
        ApmClient client = clientFactory.scheduledConnect("localhost", bindPort);

        logger.debug("close");
        ApmRPCTestUtils.close(client);
    }

    @Test
    public void scheduledConnectDelayAndClosed() throws IOException, InterruptedException {
        ApmClient client = clientFactory.scheduledConnect("localhost", bindPort);

        Thread.sleep(2000);
        logger.debug("close apm client");
        ApmRPCTestUtils.close(client);
    }

    @Test
    public void scheduledConnectStateTest() {
        ApmClient client = clientFactory.scheduledConnect("localhost", bindPort);

        client.send(new byte[10]);

        try {
            Future future = client.sendAsync(new byte[10]);
            future.await();
            future.getResult();
            Assert.fail();
        } catch (ApmSocketException e) {
        }

        try {
            client.sendSync(new byte[10]);
            Assert.fail();
        } catch (ApmSocketException e) {
        }

        try {
            ApmRPCTestUtils.request(client, new byte[10]);
            Assert.fail();
        } catch (ApmSocketException e) {
        }

        ApmRPCTestUtils.close(client);
    }

    @Test
    public void serverFirstClose() throws IOException, InterruptedException {
        // when abnormal case in which server has been closed first, confirm that a socket should be closed properly.
        ApmServerAcceptor serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort);
        ApmClient client = clientFactory.connect("127.0.0.1", bindPort);

        byte[] randomByte = TestByteUtils.createRandomByte(10);
        Future<ResponseMessage> response = client.request(randomByte);
        response.await();
        try {
            response.getResult();
        } catch (Exception e) {
            logger.debug("timeout.", e);
        }
        // close server by force
        ApmRPCTestUtils.close(serverAcceptor);
        Thread.sleep(1000*2);
        ApmRPCTestUtils.close(client);
    }

    @Test
    public void serverCloseAndWrite() throws IOException, InterruptedException {
        // when abnormal case in which server has been closed first, confirm that a client socket should be closed properly.
        ApmServerAcceptor serverAcceptor = ApmRPCTestUtils.createApmServerFactory(bindPort);
        
        ApmClient client = clientFactory.connect("127.0.0.1", bindPort);

        // just close server and request
        ApmRPCTestUtils.close(serverAcceptor);

        byte[] randomByte = TestByteUtils.createRandomByte(10);
        Future<ResponseMessage> response = client.request(randomByte);
        response.await();
        try {
            response.getResult();
            Assert.fail("expected exception");
        } catch (Exception e) {
        }

        Thread.sleep(1000 * 3);
        ApmRPCTestUtils.close(client);
    }

}
