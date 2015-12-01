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
public class ApmClient implements ApmSocket {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private volatile ApmClientHandler apmClientHandler;

    private volatile boolean closed;
    
    private List<ApmClientReconnectEventListener> reconnectEventListeners = new CopyOnWriteArrayList<ApmClientReconnectEventListener>();
    
    public ApmClient() {
        this(new ReconnectStateClientHandler());
    }

    public ApmClient(ApmClientHandler apmClientHandler) {
        AssertUtils.assertNotNull(apmClientHandler, "apmClientHandler");

        this.apmClientHandler = apmClientHandler;
        apmClientHandler.setApmClient(this);
    }

    void reconnectSocketHandler(ApmClientHandler apmClientHandler) {
        AssertUtils.assertNotNull(apmClientHandler, "apmClientHandler");

        if (closed) {
            logger.warn("reconnectClientHandler(). apmClientHandler force close.");
            apmClientHandler.close();
            return;
        }
        logger.warn("reconnectClientHandler:{}", apmClientHandler);
        
        this.apmClientHandler = apmClientHandler;
        
        notifyReconnectEvent();
    }
    

    /*
        because reconnectEventListener's constructor contains Dummy and can't be access through setter,
        guarantee it is not null.
    */
    public boolean addApmClientReconnectEventListener(ApmClientReconnectEventListener eventListener) {
        if (eventListener == null) {
            return false;
        }

        return this.reconnectEventListeners.add(eventListener);
    }

    public boolean removeApmClientReconnectEventListener(ApmClientReconnectEventListener eventListener) {
        if (eventListener == null) {
            return false;
        }

        return this.reconnectEventListeners.remove(eventListener);
    }

    private void notifyReconnectEvent() {
        for (ApmClientReconnectEventListener eachListener : this.reconnectEventListeners) {
            eachListener.reconnectPerformed(this);
        }
    }

    public void sendSync(byte[] bytes) {
        ensureOpen();
        apmClientHandler.sendSync(bytes);
    }

    public Future sendAsync(byte[] bytes) {
        ensureOpen();
        return apmClientHandler.sendAsync(bytes);
    }

    @Override
    public void send(byte[] bytes) {
        ensureOpen();
        apmClientHandler.send(bytes);
    }

    @Override
    public Future<ResponseMessage> request(byte[] bytes) {
        if (apmClientHandler == null) {
            return returnFailureFuture();
        }
        return apmClientHandler.request(bytes);
    }

    @Override
    public void response(RequestPacket requestPacket, byte[] payload) {
        response(requestPacket.getRequestId(), payload);
    }

    @Override
    public void response(int requestId, byte[] payload) {
        ensureOpen();
        apmClientHandler.response(requestId, payload);
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
        return apmClientHandler.openStream(payload, messageListener, stateChangeListener);
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return apmClientHandler.getRemoteAddress();
    }

    @Override
    public ClusterOption getLocalClusterOption() {
        return apmClientHandler.getLocalClusterOption();
    }

    @Override
    public ClusterOption getRemoteClusterOption() {
        return apmClientHandler.getRemoteClusterOption();
    }

    public StreamChannelContext findStreamChannel(int streamChannelId) {

        ensureOpen();
        return apmClientHandler.findStreamChannel(streamChannelId);
    }

    private Future<ResponseMessage> returnFailureFuture() {
        DefaultFuture<ResponseMessage> future = new DefaultFuture<ResponseMessage>();
        future.setFailure(new ApmSocketException("apmClientHandler is null"));
        return future;
    }

    private void ensureOpen() {
        if (apmClientHandler == null) {
            throw new ApmSocketException("apmClientHandler is null");
        }
    }

    /**
     * write ping packet on tcp channel
     * ApmSocketException throws when writing fails.
     *
     */
    public void sendPing() {
        ApmClientHandler apmClientHandler = this.apmClientHandler;
        if (apmClientHandler == null) {
            return;
        }
        apmClientHandler.sendPing();
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
        }
        ApmClientHandler apmClientHandler = this.apmClientHandler;
        if (apmClientHandler == null) {
            return;
        }
        apmClientHandler.close();
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isConnected() {
        return this.apmClientHandler.isConnected();
    }
}
