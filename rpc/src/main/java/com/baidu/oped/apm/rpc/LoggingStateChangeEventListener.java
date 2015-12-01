/*
 *
 *  * Copyright 2014 NAVER Corp.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 *
 */

package com.baidu.oped.apm.rpc;

import com.baidu.oped.apm.rpc.common.SocketStateCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author Taejin Koo
 */
public class LoggingStateChangeEventListener implements StateChangeEventListener<ApmSocket> {

    public static final LoggingStateChangeEventListener INSTANCE = new LoggingStateChangeEventListener();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void eventPerformed(ApmSocket apmSocket, SocketStateCode stateCode) throws Exception {
        logger.info("eventPerformed socket:{}, stateCode:{}", apmSocket, stateCode);
    }

    @Override
    public void exceptionCaught(ApmSocket apmSocket, SocketStateCode stateCode, Throwable e) {
        logger.warn("exceptionCaught message:{}, socket:{}, stateCode:{}", e.getMessage(), apmSocket, stateCode, e);
    }

}
