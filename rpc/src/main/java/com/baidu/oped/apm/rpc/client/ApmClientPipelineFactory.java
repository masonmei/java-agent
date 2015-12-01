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


import com.baidu.oped.apm.rpc.codec.PacketDecoder;
import com.baidu.oped.apm.rpc.codec.PacketEncoder;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.timeout.WriteTimeoutHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author emeroad
 * @author koo.taejin
 */
public class ApmClientPipelineFactory implements ChannelPipelineFactory {

    private final ApmClientFactory apmClientFactory;

    public ApmClientPipelineFactory(ApmClientFactory apmClientFactory) {
        if (apmClientFactory == null) {
            throw new NullPointerException("apmClientFactory must not be null");
        }
        this.apmClientFactory = apmClientFactory;
    }


    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("encoder", new PacketEncoder());
        pipeline.addLast("decoder", new PacketDecoder());
        
        long pingDelay = apmClientFactory.getPingDelay();
        long enableWorkerPacketDelay = apmClientFactory.getEnableWorkerPacketDelay();
        long timeoutMillis = apmClientFactory.getTimeoutMillis();
        
        DefaultApmClientHandler defaultApmClientHandler = new DefaultApmClientHandler(apmClientFactory, pingDelay, enableWorkerPacketDelay, timeoutMillis);
        pipeline.addLast("writeTimeout", new WriteTimeoutHandler(defaultApmClientHandler.getChannelTimer(), 3000, TimeUnit.MILLISECONDS));
        pipeline.addLast("socketHandler", defaultApmClientHandler);
        
        return pipeline;
    }
}
