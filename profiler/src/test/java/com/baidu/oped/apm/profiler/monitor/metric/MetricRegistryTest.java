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

package com.baidu.oped.apm.profiler.monitor.metric;

import static com.baidu.oped.apm.common.trace.ServiceTypeProperty.*;

import com.baidu.oped.apm.common.trace.ServiceTypeFactory;
import org.junit.Assert;
import org.junit.Test;

import com.baidu.oped.apm.common.trace.ServiceType;

public class MetricRegistryTest {
    private static final ServiceType ASYNC_HTTP_CLIENT = ServiceTypeFactory.of(9056, "ASYNC_HTTP_CLIENT", RECORD_STATISTICS);
    
    @Test
    public void testSuccess() {
        MetricRegistry metricRegistry = new MetricRegistry(ServiceType.STAND_ALONE);
        RpcMetric rpcMetric = metricRegistry.getRpcMetric(ASYNC_HTTP_CLIENT);
    }

    @Test
    public void testFalse() {
        MetricRegistry metricRegistry = null;
        try {
            metricRegistry = new MetricRegistry(ServiceType.UNKNOWN_DB);
            Assert.fail();
        } catch (Exception e) {
        }

        metricRegistry = new MetricRegistry(ServiceType.STAND_ALONE);
        try {
            metricRegistry.getRpcMetric(ServiceType.ASYNC);
            Assert.fail();
        } catch (Exception e) {
        }

    }

}