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

import com.baidu.oped.apm.rpc.StateChangeEventListener;
import com.baidu.oped.apm.rpc.common.SocketStateCode;
import com.baidu.oped.apm.rpc.server.ApmServer;

/**
 * @author koo.taejin
 */
public interface ServerStateChangeEventHandler extends StateChangeEventListener<ApmServer> {

    void eventPerformed(ApmServer apmServer, SocketStateCode stateCode) throws Exception;
    
    void exceptionCaught(ApmServer apmServer, SocketStateCode stateCode, Throwable e);

}
