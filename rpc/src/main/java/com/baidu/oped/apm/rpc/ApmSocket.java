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

package com.baidu.oped.apm.rpc;

import com.baidu.oped.apm.rpc.cluster.ClusterOption;
import com.baidu.oped.apm.rpc.packet.RequestPacket;
import com.baidu.oped.apm.rpc.stream.ClientStreamChannel;
import com.baidu.oped.apm.rpc.stream.ClientStreamChannelContext;
import com.baidu.oped.apm.rpc.stream.ClientStreamChannelMessageListener;
import com.baidu.oped.apm.rpc.stream.StreamChannelStateChangeEventHandler;

import java.net.SocketAddress;

/**
 * @Author Taejin Koo
 */
public interface ApmSocket {

    void send(byte[] payload);

    Future<ResponseMessage> request(byte[] payload);

    void response(RequestPacket requestPacket, byte[] payload);
    void response(int requestId, byte[] payload);

    ClientStreamChannelContext openStream(byte[] payload, ClientStreamChannelMessageListener messageListener);
    ClientStreamChannelContext openStream(byte[] payload, ClientStreamChannelMessageListener messageListener, StreamChannelStateChangeEventHandler<ClientStreamChannel> stateChangeListener);

    SocketAddress getRemoteAddress();

    void close();

    ClusterOption getLocalClusterOption();
    ClusterOption getRemoteClusterOption();

}
