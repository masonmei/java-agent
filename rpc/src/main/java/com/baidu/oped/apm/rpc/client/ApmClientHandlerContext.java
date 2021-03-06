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

import com.baidu.oped.apm.rpc.stream.*;
import org.jboss.netty.channel.Channel;

import com.baidu.oped.apm.rpc.packet.stream.StreamPacket;

/**
 * @author Taejin Koo
 */
public class ApmClientHandlerContext {
    private final Channel channel;
    private final StreamChannelManager streamChannelManager;

    public ApmClientHandlerContext(Channel channel, StreamChannelManager streamChannelManager) {
        if (channel == null) {
            throw new NullPointerException("channel must not be null");
        }
        if (streamChannelManager == null) {
            throw new NullPointerException("streamChannelManager must not be null");
        }
        this.channel = channel;
        this.streamChannelManager = streamChannelManager;
    }

    public Channel getChannel() {
        return channel;
    }

    public ClientStreamChannelContext openStream(byte[] payload, ClientStreamChannelMessageListener messageListener) {
        return openStream(payload, messageListener, null);
    }

    public ClientStreamChannelContext openStream(byte[] payload, ClientStreamChannelMessageListener messageListener, StreamChannelStateChangeEventHandler<ClientStreamChannel> stateChangeListener) {
        return streamChannelManager.openStream(payload, messageListener, stateChangeListener);
    }

    public void handleStreamEvent(StreamPacket message) {
        streamChannelManager.messageReceived(message);
    }

    public void closeAllStreamChannel() {
        streamChannelManager.close();
    }

    public StreamChannelContext getStreamChannel(int streamChannelId) {
        return streamChannelManager.findStreamChannel(streamChannelId);
    }
    
}
