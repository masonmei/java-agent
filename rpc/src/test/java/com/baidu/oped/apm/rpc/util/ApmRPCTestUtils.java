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

package com.baidu.oped.apm.rpc.util;

import com.baidu.oped.apm.rpc.*;
import com.baidu.oped.apm.rpc.client.ApmClient;
import com.baidu.oped.apm.rpc.client.ApmClientFactory;
import com.baidu.oped.apm.rpc.packet.*;
import com.baidu.oped.apm.rpc.server.AgentHandshakePropertyType;
import com.baidu.oped.apm.rpc.server.ApmServer;
import com.baidu.oped.apm.rpc.server.ApmServerAcceptor;
import com.baidu.oped.apm.rpc.server.ServerMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ApmRPCTestUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ApmRPCTestUtils.class);

    private ApmRPCTestUtils() {
    }
    

    public static int findAvailablePort() throws IOException {
        return findAvailablePort(21111);
    }

    public static int findAvailablePort(int defaultPort) throws IOException {
        int bindPort = defaultPort;

        ServerSocket serverSocket = null;
        while (0xFFFF >= bindPort && serverSocket == null) {
            try {
                serverSocket = new ServerSocket(bindPort);
            } catch (IOException ex) {
                bindPort++;
            }
        }
        
        if (serverSocket != null) {
            serverSocket.close();
            return bindPort;
        } 
        
        throw new IOException("can't find available port.");
    }

    public static ApmServerAcceptor createApmServerFactory(int bindPort) {
        return createApmServerFactory(bindPort, null);
    }
    
    public static ApmServerAcceptor createApmServerFactory(int bindPort, ServerMessageListener messageListener) {
        ApmServerAcceptor serverAcceptor = new ApmServerAcceptor();
        serverAcceptor.bind("127.0.0.1", bindPort);
        
        if (messageListener != null) {
            serverAcceptor.setMessageListener(messageListener);
        }

        return serverAcceptor;
    }
    
    public static void close(ApmServerAcceptor serverAcceptor, ApmServerAcceptor... serverAcceptors) {
        if (serverAcceptor != null) {
            serverAcceptor.close();
        }
        
        if (serverAcceptors != null) {
            for (ApmServerAcceptor eachServerAcceptor : serverAcceptors) {
                if (eachServerAcceptor != null) {
                    eachServerAcceptor.close();
                }
            }
        }
    }
    
    public static ApmClientFactory createClientFactory(Map param) {
        return createClientFactory(param, null);
    }
    
    public static ApmClientFactory createClientFactory(Map param, MessageListener messageListener) {
        ApmClientFactory clientFactory = new ApmClientFactory();
        clientFactory.setProperties(param);
        clientFactory.addStateChangeEventListener(LoggingStateChangeEventListener.INSTANCE);

        if (messageListener != null) {
            clientFactory.setMessageListener(messageListener);
        }
        
        return clientFactory;
    }

    public static byte[] request(ApmSocket writableServer, byte[] message) {
        Future<ResponseMessage> future = writableServer.request(message);
        future.await();
        return future.getResult().getMessage();
    }

    public static byte[] request(ApmClient client, byte[] message) {
        Future<ResponseMessage> future = client.request(message);
        future.await();
        return future.getResult().getMessage();
    }

    public static void close(ApmClient client, ApmClient... clients) {
        if (client != null) {
            client.close();
        }
        
        if (clients != null) {
            for (ApmClient eachSocket : clients) {
                if (eachSocket != null) {
                    eachSocket.close();
                }
            }
        }
    }
    
    public static void close(Socket socket, Socket... sockets) throws IOException {
        if (socket != null) {
            socket.close();
        }
        
        if (sockets != null) {
            for (Socket eachSocket : sockets) {
                if (eachSocket != null) {
                    eachSocket.close();
                }
            }
        }
    }

    
    public static EchoServerListener createEchoServerListener() {
        return new EchoServerListener();
    }

    public static EchoClientListener createEchoClientListener() {
        return new EchoClientListener();
    }

    public static Map getParams() {
        Map properties = new HashMap();
        properties.put(AgentHandshakePropertyType.AGENT_ID.getName(), "agent");
        properties.put(AgentHandshakePropertyType.APPLICATION_NAME.getName(), "application");
        properties.put(AgentHandshakePropertyType.HOSTNAME.getName(), "hostname");
        properties.put(AgentHandshakePropertyType.IP.getName(), "ip");
        properties.put(AgentHandshakePropertyType.PID.getName(), 1111);
        properties.put(AgentHandshakePropertyType.SERVICE_TYPE.getName(), 10);
        properties.put(AgentHandshakePropertyType.START_TIMESTAMP.getName(), System.currentTimeMillis());
        properties.put(AgentHandshakePropertyType.VERSION.getName(), "1.0");

        return properties;
    }

    public static class EchoServerListener implements ServerMessageListener {
        private final List<SendPacket> sendPacketRepository = new ArrayList<SendPacket>();
        private final List<RequestPacket> requestPacketRepository = new ArrayList<RequestPacket>();

        @Override
        public void handleSend(SendPacket sendPacket, ApmSocket apmSocket) {
            logger.info("handleSend packet:{}, remote:{}", sendPacket, apmSocket.getRemoteAddress());
            sendPacketRepository.add(sendPacket);
        }

        @Override
        public void handleRequest(RequestPacket requestPacket, ApmSocket apmSocket) {
            logger.info("handleRequest packet:{}, remote:{}", requestPacket, apmSocket.getRemoteAddress());

            requestPacketRepository.add(requestPacket);
            apmSocket.response(requestPacket, requestPacket.getPayload());
        }

        @Override
        public HandshakeResponseCode handleHandshake(Map properties) {
            logger.info("handle Handshake {}", properties);
            return HandshakeResponseType.Success.DUPLEX_COMMUNICATION;
        }

        @Override
        public void handlePing(PingPacket pingPacket, ApmServer apmServer) {
            
        }
    }
    
    public static class EchoClientListener implements MessageListener {
        private final List<SendPacket> sendPacketRepository = new ArrayList<SendPacket>();
        private final List<RequestPacket> requestPacketRepository = new ArrayList<RequestPacket>();

        @Override
        public void handleSend(SendPacket sendPacket, ApmSocket apmSocket) {
            sendPacketRepository.add(sendPacket);

            byte[] payload = sendPacket.getPayload();
            logger.debug(new String(payload));
        }

        @Override
        public void handleRequest(RequestPacket requestPacket, ApmSocket apmSocket) {
            requestPacketRepository.add(requestPacket);

            byte[] payload = requestPacket.getPayload();
            logger.debug(new String(payload));

            apmSocket.response(requestPacket, payload);
        }

        public List<SendPacket> getSendPacketRepository() {
            return sendPacketRepository;
        }

        public List<RequestPacket> getRequestPacketRepository() {
            return requestPacketRepository;
        }
    }

}
