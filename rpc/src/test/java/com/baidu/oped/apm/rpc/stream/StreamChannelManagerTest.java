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

package com.baidu.oped.apm.rpc.stream;

import com.baidu.oped.apm.rpc.*;
import com.baidu.oped.apm.rpc.client.ApmClient;
import com.baidu.oped.apm.rpc.client.ApmClientFactory;
import com.baidu.oped.apm.rpc.client.SimpleMessageListener;
import com.baidu.oped.apm.rpc.packet.stream.StreamClosePacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamCode;
import com.baidu.oped.apm.rpc.packet.stream.StreamCreateFailPacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamCreatePacket;
import com.baidu.oped.apm.rpc.server.ApmServer;
import com.baidu.oped.apm.rpc.server.ApmServerAcceptor;
import com.baidu.oped.apm.rpc.server.ServerMessageListener;
import com.baidu.oped.apm.rpc.server.SimpleServerMessageListener;
import com.baidu.oped.apm.rpc.util.ApmRPCTestUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class StreamChannelManagerTest {

    private static int bindPort;
    
    @BeforeClass
    public static void setUp() throws IOException {
        bindPort = ApmRPCTestUtils.findAvailablePort();
    }

    // Client to Server Stream
    @Test
    public void streamSuccessTest1() throws IOException, InterruptedException {
        SimpleStreamBO bo = new SimpleStreamBO();

        ApmServerAcceptor serverAcceptor = createServerFactory(SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE, new ServerListener(bo));
        serverAcceptor.bind("localhost", bindPort);

        ApmClientFactory clientFactory = createSocketFactory();
        try {
            ApmClient client = clientFactory.connect("127.0.0.1", bindPort);

            RecordedStreamChannelMessageListener clientListener = new RecordedStreamChannelMessageListener(4);

            ClientStreamChannelContext clientContext = client.openStream(new byte[0], clientListener);

            int sendCount = 4;

            for (int i = 0; i < sendCount; i++) {
                sendRandomBytes(bo);
            }

            Thread.sleep(100);

            Assert.assertEquals(sendCount, clientListener.getReceivedMessage().size());

            clientContext.getStreamChannel().close();
            
            ApmRPCTestUtils.close(client);
        } finally {
            clientFactory.release();
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    // Client to Server Stream
    @Test
    public void streamSuccessTest2() throws IOException, InterruptedException {
        SimpleStreamBO bo = new SimpleStreamBO();

        ApmServerAcceptor serverAcceptor = createServerFactory(SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE, new ServerListener(bo));
        serverAcceptor.bind("localhost", bindPort);

        ApmClientFactory clientFactory = createSocketFactory();
        try {
            ApmClient client = clientFactory.connect("127.0.0.1", bindPort);

            RecordedStreamChannelMessageListener clientListener = new RecordedStreamChannelMessageListener(4);
            ClientStreamChannelContext clientContext = client.openStream(new byte[0], clientListener);

            RecordedStreamChannelMessageListener clientListener2 = new RecordedStreamChannelMessageListener(4);
            ClientStreamChannelContext clientContext2 = client.openStream(new byte[0], clientListener2);


            int sendCount = 4;
            for (int i = 0; i < sendCount; i++) {
                sendRandomBytes(bo);
            }

            Thread.sleep(100);

            Assert.assertEquals(sendCount, clientListener.getReceivedMessage().size());
            Assert.assertEquals(sendCount, clientListener2.getReceivedMessage().size());

            clientContext.getStreamChannel().close();

            Thread.sleep(100);

            sendCount = 4;
            for (int i = 0; i < sendCount; i++) {
                sendRandomBytes(bo);
            }

            Thread.sleep(100);

            Assert.assertEquals(sendCount, clientListener.getReceivedMessage().size());
            Assert.assertEquals(8, clientListener2.getReceivedMessage().size());


            clientContext2.getStreamChannel().close();

            ApmRPCTestUtils.close(client);
        } finally {
            clientFactory.release();
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void streamSuccessTest3() throws IOException, InterruptedException {
        ApmServerAcceptor serverAcceptor = createServerFactory(SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE, null);
        serverAcceptor.bind("localhost", bindPort);

        SimpleStreamBO bo = new SimpleStreamBO();

        ApmClientFactory clientFactory = createSocketFactory(SimpleMessageListener.INSTANCE, new ServerListener(bo));

        try {
            ApmClient client = clientFactory.connect("127.0.0.1", bindPort);

            Thread.sleep(100);

            List<ApmSocket> writableServerList = serverAcceptor.getWritableSocketList();
            Assert.assertEquals(1, writableServerList.size());

            ApmSocket writableServer = writableServerList.get(0);

            RecordedStreamChannelMessageListener clientListener = new RecordedStreamChannelMessageListener(4);

            if (writableServer instanceof  ApmServer) {
                ClientStreamChannelContext clientContext = ((ApmServer)writableServer).openStream(new byte[0], clientListener);

                int sendCount = 4;

                for (int i = 0; i < sendCount; i++) {
                    sendRandomBytes(bo);
                }

                Thread.sleep(100);

                Assert.assertEquals(sendCount, clientListener.getReceivedMessage().size());

                clientContext.getStreamChannel().close();
            } else {
                Assert.fail();
            }

            ApmRPCTestUtils.close(client);
        } finally {
            clientFactory.release();
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void streamClosedTest1() throws IOException, InterruptedException {
        ApmServerAcceptor serverAcceptor = createServerFactory(SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE, null);
        serverAcceptor.bind("localhost", bindPort);

        ApmClientFactory clientFactory = createSocketFactory();
        try {
            ApmClient client = clientFactory.connect("127.0.0.1", bindPort);

            RecordedStreamChannelMessageListener clientListener = new RecordedStreamChannelMessageListener(4);

            ClientStreamChannelContext clientContext = client.openStream(new byte[0], clientListener);

            Thread.sleep(100);

            StreamCreateFailPacket createFailPacket = clientContext.getCreateFailPacket();
            if (createFailPacket == null) {
                Assert.fail();
            }

            clientContext.getStreamChannel().close();
            
            ApmRPCTestUtils.close(client);
        } finally {
            clientFactory.release();
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    @Test
    public void streamClosedTest2() throws IOException, InterruptedException {
        SimpleStreamBO bo = new SimpleStreamBO();

        ApmServerAcceptor serverAcceptor = createServerFactory(SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE, new ServerListener(bo));
        serverAcceptor.bind("localhost", bindPort);

        ApmClientFactory clientFactory = createSocketFactory();

        ApmClient client = null;
        try {
            client = clientFactory.connect("127.0.0.1", bindPort);

            RecordedStreamChannelMessageListener clientListener = new RecordedStreamChannelMessageListener(4);

            ClientStreamChannelContext clientContext = client.openStream(new byte[0], clientListener);
            Thread.sleep(100);

            Assert.assertEquals(1, bo.getStreamChannelContextSize());

            clientContext.getStreamChannel().close();
            Thread.sleep(100);

            Assert.assertEquals(0, bo.getStreamChannelContextSize());

        } finally {
            ApmRPCTestUtils.close(client);
            clientFactory.release();
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    // ServerSocket to Client Stream


    // ServerStreamChannel first close.
    @Test(expected = ApmSocketException.class)
    public void streamClosedTest3() throws IOException, InterruptedException {
        ApmServerAcceptor serverAcceptor = createServerFactory(SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE, null);
        serverAcceptor.bind("localhost", bindPort);

        SimpleStreamBO bo = new SimpleStreamBO();

        ApmClientFactory clientFactory = createSocketFactory(SimpleMessageListener.INSTANCE, new ServerListener(bo));

        ApmClient client = clientFactory.connect("127.0.0.1", bindPort);
        try {

            Thread.sleep(100);

            List<ApmSocket> writableServerList = serverAcceptor.getWritableSocketList();
            Assert.assertEquals(1, writableServerList.size());

            ApmSocket writableServer = writableServerList.get(0);

            if (writableServer instanceof  ApmServer) {
                RecordedStreamChannelMessageListener clientListener = new RecordedStreamChannelMessageListener(4);

                ClientStreamChannelContext clientContext = ((ApmServer)writableServer).openStream(new byte[0], clientListener);


                StreamChannelContext aaa = client.findStreamChannel(2);

                aaa.getStreamChannel().close();

                sendRandomBytes(bo);

                Thread.sleep(100);


                clientContext.getStreamChannel().close();
            } else {
                Assert.fail();
            }

        } finally {
            ApmRPCTestUtils.close(client);
            clientFactory.release();
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }


    private ApmServerAcceptor createServerFactory(ServerMessageListener severMessageListener, ServerStreamChannelMessageListener serverStreamChannelMessageListener) {
        ApmServerAcceptor serverAcceptor = new ApmServerAcceptor();

        if (severMessageListener != null) {
            serverAcceptor.setMessageListener(severMessageListener);
        }

        if (serverStreamChannelMessageListener != null) {
            serverAcceptor.setServerStreamChannelMessageListener(serverStreamChannelMessageListener);
        }

        return serverAcceptor;
    }

    private ApmClientFactory createSocketFactory() {
        ApmClientFactory clientFactory = new ApmClientFactory();
        return clientFactory;
    }

    private ApmClientFactory createSocketFactory(MessageListener messageListener, ServerStreamChannelMessageListener serverStreamChannelMessageListener) {
        ApmClientFactory clientFactory = new ApmClientFactory();
        clientFactory.setMessageListener(messageListener);
        clientFactory.setServerStreamChannelMessageListener(serverStreamChannelMessageListener);

        return clientFactory;
    }

    private void sendRandomBytes(SimpleStreamBO bo) {
        byte[] openBytes = TestByteUtils.createRandomByte(30);
        bo.sendResponse(openBytes);
    }

    class ServerListener implements ServerStreamChannelMessageListener {

        private final SimpleStreamBO bo;

        public ServerListener(SimpleStreamBO bo) {
            this.bo = bo;
        }

        @Override
        public StreamCode handleStreamCreate(ServerStreamChannelContext streamChannelContext, StreamCreatePacket packet) {
            bo.addServerStreamChannelContext(streamChannelContext);
            return StreamCode.OK;
        }

        @Override
        public void handleStreamClose(ServerStreamChannelContext streamChannelContext, StreamClosePacket packet) {
            bo.removeServerStreamChannelContext(streamChannelContext);
        }

    }

    class SimpleStreamBO {

        private final List<ServerStreamChannelContext> serverStreamChannelContextList;

        public SimpleStreamBO() {
            serverStreamChannelContextList = new CopyOnWriteArrayList<ServerStreamChannelContext>();
        }

        public void addServerStreamChannelContext(ServerStreamChannelContext context) {
            serverStreamChannelContextList.add(context);
        }

        public void removeServerStreamChannelContext(ServerStreamChannelContext context) {
            serverStreamChannelContextList.remove(context);
        }

        void sendResponse(byte[] data) {

            for (ServerStreamChannelContext context : serverStreamChannelContextList) {
                context.getStreamChannel().sendData(data);
            }
        }

        int getStreamChannelContextSize() {
            return serverStreamChannelContextList.size();
        }
    }

}
