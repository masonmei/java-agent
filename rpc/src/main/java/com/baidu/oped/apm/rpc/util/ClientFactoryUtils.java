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

package com.baidu.oped.apm.rpc.util;

import com.baidu.oped.apm.rpc.ApmSocketException;
import com.baidu.oped.apm.rpc.client.ApmClient;
import com.baidu.oped.apm.rpc.client.ApmClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @Author Taejin Koo
 */
public final class ClientFactoryUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientFactoryUtils.class);

    public static ApmClient createApmClient(String host, int port, ApmClientFactory clientFactory) {
        InetSocketAddress connectAddress = new InetSocketAddress(host, port);
        return createApmClient(connectAddress, clientFactory);
    }

    public static ApmClient createApmClient(InetSocketAddress connectAddress, ApmClientFactory clientFactory) {
        ApmClient apmClient = null;
        for (int i = 0; i < 3; i++) {
            try {
                apmClient = clientFactory.connect(connectAddress);
                LOGGER.info("tcp connect success. remote:{}", connectAddress);
                return apmClient;
            } catch (ApmSocketException e) {
                LOGGER.warn("tcp connect fail. retmoe:{} try reconnect, retryCount:{}", connectAddress, i);
            }
        }
        LOGGER.warn("change background tcp connect mode remote:{} ", connectAddress);
        apmClient = clientFactory.scheduledConnect(connectAddress);

        return apmClient;
    }

}
