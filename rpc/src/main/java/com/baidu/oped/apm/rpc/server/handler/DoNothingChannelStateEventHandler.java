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

package com.baidu.oped.apm.rpc.server.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.rpc.common.SocketStateCode;
import com.baidu.oped.apm.rpc.server.ApmServer;

/**
 * @author koo.taejin
 */
public class DoNothingChannelStateEventHandler implements ServerStateChangeEventHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final ServerStateChangeEventHandler INSTANCE = new DoNothingChannelStateEventHandler();

    @Override
    public void eventPerformed(ApmServer apmServer, SocketStateCode stateCode) {
        logger.info("{} eventPerformed(). apmServer:{}, code:{}", this.getClass().getSimpleName(), apmServer, stateCode);
    }
    
    @Override
    public void exceptionCaught(ApmServer apmServer, SocketStateCode stateCode, Throwable e) {
        logger.warn("{} exceptionCaught(). apmServer:{}, code:{}. Error: {}.", this.getClass().getSimpleName(), apmServer, stateCode, e.getMessage(), e);
    }

}
