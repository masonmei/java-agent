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

import com.baidu.oped.apm.rpc.*;
import com.baidu.oped.apm.rpc.cluster.ClusterOption;
import com.baidu.oped.apm.rpc.packet.RequestPacket;
import com.baidu.oped.apm.rpc.stream.*;
import com.baidu.oped.apm.rpc.util.AssertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * @author emeroad
 * @author koo.taejin
 * @author netspider
 */
public class PinpointClient implements PinpointSocket {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private volatile PinpointClientHandler pinpointClientHandler;

    private volatile boolean closed;
    
    private List<PinpointClientReconnectEventListener> reconnectEventListeners = new CopyOnWriteArrayList<PinpointClientReconnectEventListener>();
    
    public PinpointClient() {
        this(new ReconnectStateClientHandler());
    }

    public PinpointClient(PinpointClientHandler pinpointClientHandler) {
        AssertUtils.assertNotNull(pinpointClientHandler, "pinpointClientHandler");

        this.pinpointClientHandler = pinpointClientHandler;
        pinpointClientHandler.setPinpointClient(this);
    }

    void reconnectSocketHandler(PinpointClientHandler pinpointClientHandler) {
        AssertUtils.assertNotNull(pinpointClientHandler, "pinpointClientHandler");

        if (closed) {
            logger.warn("reconnectClientHandler(). pinpointClientHandler force close.");
            pinpointClientHandler.close();
            return;
        }
        logger.warn("reconnectClientHandler:{}", pinpointClientHandler);
        
        this.pinpointClientHandler = pinpointClientHandler;
        
        notifyReconnectEvent();
    }
    

    /*
        because reconnectEventListener's constructor contains Dummy and can't be access through setter,
        guarantee it is not null.
    */
    public boolean addPinpointClientReconnectEventListener(PinpointClientReconnectEventListener eventListener) {
        if (eventListener == null) {
            return false;
        }

        return this.reconnectEventListeners.add(eventListener);
    }

    public boolean removePinpointClientReconnectEventListener(PinpointClientReconnectEventListener eventListener) {
        if (eventListener == null) {
            return false;
        }

        return this.reconnectEventListeners.remove(eventListener);
    }

    private void notifyReconnectEvent() {
        for (PinpointClientReconnectEventListener eachListener : this.reconnectEventListeners) {
            eachListener.reconnectPerformed(this);
        }
    }

    public void sendSync(byte[] bytes) {
        ensureOpen();
        pinpointClientHandler.sendSync(bytes);
    }

    public Future sendAsync(byte[] bytes) {
        ensureOpen();
        return pinpointClientHandler.sendAsync(bytes);
    }

    @Override
    public void send(byte[] bytes) {
        ensureOpen();
        pinpointClientHandler.send(bytes);
    }

    @Override
    public Future<ResponseMessage> request(byte[] bytes) {
        if (pinpointClientHandler == null) {
            return returnFailureFuture();
        }
        return pinpointClientHandler.request(bytes);
    }

    @Override
    public void response(RequestPacket requestPacket, byte[] payload) {
        response(requestPacket.getRequestId(), payload);
    }

    @Override
    public void response(int requestId, byte[] payload) {
        ensureOpen();
        pinpointClientHandler.response(requestId, payload);
    }

    @Override
    public ClientStreamChannelContext openStream(byte[] payload, ClientStreamChannelMessageListener messageListener) {
        return openStream(payload, messageListener, null);
    }

    @Override
    public ClientStreamChannelContext openStream(byte[] payload, ClientStreamChannelMessageListener messageListener, StreamChannelStateChangeEventHandler<ClientStreamChannel> stateChangeListener) {
        // StreamChannel must be changed into interface in order to throw the StreamChannel that returns failure.
        // fow now throw just exception
        ensureOpen();
        return pinpointClientHandler.openStream(payload, messageListener, stateChangeListener);
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return pinpointClientHandler.getRemoteAddress();
    }

    @Override
    public ClusterOption getLocalClusterOption() {
        return pinpointClientHandler.getLocalClusterOption();
    }

    @Override
    public ClusterOption getRemoteClusterOption() {
        return pinpointClientHandler.getRemoteClusterOption();
    }

    public StreamChannelContext findStreamChannel(int streamChannelId) {

        ensureOpen();
        return pinpointClientHandler.findStreamChannel(streamChannelId);
    }

    private Future<ResponseMessage> returnFailureFuture() {
        DefaultFuture<ResponseMessage> future = new DefaultFuture<ResponseMessage>();
        future.setFailure(new PinpointSocketException("pinpointClientHandler is null"));
        return future;
    }

    private void ensureOpen() {
        if (pinpointClientHandler == null) {
            throw new PinpointSocketException("pinpointClientHandler is null");
        }
    }

    /**
     * write ping packet on tcp channel
     * PinpointSocketException throws when writing fails.
     *
     */
    public void sendPing() {
        PinpointClientHandler pinpointClientHandler = this.pinpointClientHandler;
        if (pinpointClientHandler == null) {
            return;
        }
        pinpointClientHandler.sendPing();
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
        }
        PinpointClientHandler pinpointClientHandler = this.pinpointClientHandler;
        if (pinpointClientHandler == null) {
            return;
        }
        pinpointClientHandler.close();
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isConnected() {
        return this.pinpointClientHandler.isConnected();
    }
}
