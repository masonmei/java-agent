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

import com.baidu.oped.apm.rpc.ApmSocket;
import com.baidu.oped.apm.rpc.common.SocketStateCode;
import com.baidu.oped.apm.rpc.control.ProtocolException;
import com.baidu.oped.apm.rpc.packet.*;
import com.baidu.oped.apm.rpc.server.handler.ServerStateChangeEventHandler;
import com.baidu.oped.apm.rpc.util.ControlMessageEncodingUtils;
import com.baidu.oped.apm.rpc.util.MapUtils;
import com.baidu.oped.apm.rpc.util.ApmRPCTestUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author koo.taejin
 */
public class EventHandlerTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static int bindPort;
    
    @BeforeClass
    public static void setUp() throws IOException {
        bindPort = ApmRPCTestUtils.findAvailablePort();
    }

    // Test for being possible to send messages in case of failure of registering packet ( return code : 2, lack of parameter)
    @Test
    public void registerAgentSuccessTest() throws Exception {
        EventHandler eventHandler = new EventHandler();

        ApmServerAcceptor serverAcceptor = new ApmServerAcceptor();
        serverAcceptor.addStateChangeEventHandler(eventHandler);
        serverAcceptor.setMessageListener(SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE);
        serverAcceptor.bind("127.0.0.1", bindPort);

        Socket socket = null;
        try {
            socket = new Socket("127.0.0.1", bindPort);
            sendAndReceiveSimplePacket(socket);
            Assert.assertEquals(eventHandler.getCode(), SocketStateCode.RUN_WITHOUT_HANDSHAKE);

            int code = sendAndReceiveRegisterPacket(socket, ApmRPCTestUtils.getParams());
            Assert.assertEquals(eventHandler.getCode(), SocketStateCode.RUN_DUPLEX);

            sendAndReceiveSimplePacket(socket);
        } finally {
            if (socket != null) {
                socket.close();
            }
            
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }
    
    @Test
    public void registerAgentFailTest() throws Exception {
        ThrowExceptionEventHandler eventHandler = new ThrowExceptionEventHandler();

        ApmServerAcceptor serverAcceptor = new ApmServerAcceptor();
        serverAcceptor.addStateChangeEventHandler(eventHandler);
        serverAcceptor.setMessageListener(SimpleServerMessageListener.DUPLEX_ECHO_INSTANCE);
        serverAcceptor.bind("127.0.0.1", bindPort);

        Socket socket = null;
        try {
            socket = new Socket("127.0.0.1", bindPort);
            sendAndReceiveSimplePacket(socket);
            
            Assert.assertTrue(eventHandler.getErrorCount() > 0);
        } finally {
            if (socket != null) {
                socket.close();
            }
            
            ApmRPCTestUtils.close(serverAcceptor);
        }
    }

    private int sendAndReceiveRegisterPacket(Socket socket, Map<String, Object> properties) throws ProtocolException, IOException {
        sendRegisterPacket(socket.getOutputStream(), properties);
        ControlHandshakeResponsePacket packet = receiveRegisterConfirmPacket(socket.getInputStream());
        Map<Object, Object> result = (Map<Object, Object>) ControlMessageEncodingUtils.decode(packet.getPayload());

        return MapUtils.getInteger(result, "code", -1);
    }

    private void sendAndReceiveSimplePacket(Socket socket) throws ProtocolException, IOException {
        sendSimpleRequestPacket(socket.getOutputStream());
        ResponsePacket responsePacket = readSimpleResponsePacket(socket.getInputStream());
        Assert.assertNotNull(responsePacket);
    }

    private void sendRegisterPacket(OutputStream outputStream, Map<String, Object> properties) throws ProtocolException, IOException {
        byte[] payload = ControlMessageEncodingUtils.encode(properties);
        ControlHandshakePacket packet = new ControlHandshakePacket(1, payload);

        ByteBuffer bb = packet.toBuffer().toByteBuffer(0, packet.toBuffer().writerIndex());
        sendData(outputStream, bb.array());
    }

    private void sendSimpleRequestPacket(OutputStream outputStream) throws ProtocolException, IOException {
        RequestPacket packet = new RequestPacket(new byte[0]);
        packet.setRequestId(10);

        ByteBuffer bb = packet.toBuffer().toByteBuffer(0, packet.toBuffer().writerIndex());
        sendData(outputStream, bb.array());
    }

    private void sendData(OutputStream outputStream, byte[] payload) throws IOException {
        outputStream.write(payload);
        outputStream.flush();
    }

    private ControlHandshakeResponsePacket receiveRegisterConfirmPacket(InputStream inputStream) throws ProtocolException, IOException {

        byte[] payload = readData(inputStream);
        ChannelBuffer cb = ChannelBuffers.wrappedBuffer(payload);

        short packetType = cb.readShort();

        ControlHandshakeResponsePacket packet = ControlHandshakeResponsePacket.readBuffer(packetType, cb);
        return packet;
    }

    private ResponsePacket readSimpleResponsePacket(InputStream inputStream) throws ProtocolException, IOException {
        byte[] payload = readData(inputStream);
        ChannelBuffer cb = ChannelBuffers.wrappedBuffer(payload);

        short packetType = cb.readShort();

        ResponsePacket packet = ResponsePacket.readBuffer(packetType, cb);
        return packet;
    }

    private byte[] readData(InputStream inputStream) throws IOException {
        int availableSize = 0;

        for (int i = 0; i < 3; i++) {
            availableSize = inputStream.available();

            if (availableSize > 0) {
                break;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        byte[] payload = new byte[availableSize];
        inputStream.read(payload);

        return payload;
    }

    class EventHandler implements ServerStateChangeEventHandler {

        private SocketStateCode code;

        @Override
        public void eventPerformed(ApmServer apmServer, SocketStateCode stateCode) {
            this.code = stateCode;
        }
        
        @Override
        public void exceptionCaught(ApmServer apmServer, SocketStateCode stateCode, Throwable e) {
        }

        public SocketStateCode getCode() {
            return code;
        }
    }
    
    class ThrowExceptionEventHandler implements ServerStateChangeEventHandler {

        private int errorCount = 0;
        
        @Override
        public void eventPerformed(ApmServer apmServer, SocketStateCode stateCode) throws Exception {
            throw new Exception("always error.");
        }

        @Override
        public void exceptionCaught(ApmServer apmServer, SocketStateCode stateCode, Throwable e) {
            errorCount++;
        }

        public int getErrorCount() {
            return errorCount;
        }

    }

}
