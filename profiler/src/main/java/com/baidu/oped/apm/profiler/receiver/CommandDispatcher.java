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

package com.baidu.oped.apm.profiler.receiver;

import com.baidu.oped.apm.rpc.MessageListener;
import com.baidu.oped.apm.rpc.PinpointSocket;
import com.baidu.oped.apm.rpc.packet.RequestPacket;
import com.baidu.oped.apm.rpc.packet.SendPacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamClosePacket;
import com.baidu.oped.apm.rpc.packet.stream.StreamCode;
import com.baidu.oped.apm.rpc.packet.stream.StreamCreatePacket;
import com.baidu.oped.apm.rpc.stream.ServerStreamChannelContext;
import com.baidu.oped.apm.rpc.stream.ServerStreamChannelMessageListener;
import com.baidu.oped.apm.thrift.dto.TResult;
import com.baidu.oped.apm.thrift.util.SerializationUtils;
import org.apache.thrift.TBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Taejin Koo
 */
public class CommandDispatcher implements MessageListener, ServerStreamChannelMessageListener  {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ProfilerCommandServiceRegistry commandServiceRegistry = new ProfilerCommandServiceRegistry();

    public CommandDispatcher() {
    }

    @Override
    public void handleSend(SendPacket sendPacket, PinpointSocket pinpointSocket) {
        logger.info("handleSend packet:{}, remote:{}", sendPacket, pinpointSocket.getRemoteAddress());
    }

    @Override
    public void handleRequest(RequestPacket requestPacket, PinpointSocket pinpointSocket) {
        logger.info("handleRequest packet:{}, remote:{}", requestPacket, pinpointSocket.getRemoteAddress());

        final TBase<?, ?> request = SerializationUtils.deserialize(requestPacket.getPayload(), CommandSerializer.DESERIALIZER_FACTORY, null);
        logger.debug("handleRequest request:{}, remote:{}", request, pinpointSocket.getRemoteAddress());

        TBase response;
        if (request == null) {
            final TResult tResult = new TResult(false);
            tResult.setMessage("Unsupported ServiceTypeInfo.");

            response = tResult;
        } else {
            final ProfilerRequestCommandService service = commandServiceRegistry.getRequestService(request);
            if (service == null) {
                TResult tResult = new TResult(false);
                tResult.setMessage("Can't find suitable service(" + request + ").");

                response = tResult;
            } else {
                response = service.requestCommandService(request);
            }
        }

        final byte[] payload = SerializationUtils.serialize(response, CommandSerializer.SERIALIZER_FACTORY, null);
        if (payload != null) {
            pinpointSocket.response(requestPacket, payload);
        }
    }

    @Override
    public StreamCode handleStreamCreate(ServerStreamChannelContext streamChannelContext, StreamCreatePacket packet) {
        logger.info("MessageReceived handleStreamCreate {} {}", packet, streamChannelContext);

        final TBase<?, ?> request = SerializationUtils.deserialize(packet.getPayload(), CommandSerializer.DESERIALIZER_FACTORY, null);
        if (request == null) {
            return StreamCode.TYPE_UNKNOWN;
        }
        
        final ProfilerStreamCommandService service = commandServiceRegistry.getStreamService(request);
        if (service == null) {
            return StreamCode.TYPE_UNSUPPORT;
        }
        
        return service.streamCommandService(request, streamChannelContext);
    }

    @Override
    public void handleStreamClose(ServerStreamChannelContext streamChannelContext, StreamClosePacket packet) {
    }

    public boolean registerCommandService(ProfilerCommandService commandService) {
        if (commandService == null) {
            throw new NullPointerException("commandService must not be null");
        }
        return this.commandServiceRegistry.addService(commandService);
    }

    public void registerCommandService(ProfilerCommandServiceGroup commandServiceGroup) {
        if (commandServiceGroup == null) {
            throw new NullPointerException("commandServiceGroup must not be null");
        }
        this.commandServiceRegistry.addService(commandServiceGroup);
    }

}
