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

package com.baidu.oped.apm.profiler.context;

import java.util.Collections;

import com.baidu.oped.apm.bootstrap.context.ServerMetaDataHolder;
import com.baidu.oped.apm.bootstrap.context.Trace;
import com.baidu.oped.apm.common.Version;
import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.common.util.JvmUtils;
import com.baidu.oped.apm.common.util.SystemPropertyKey;
import com.baidu.oped.apm.profiler.AgentInformation;
import com.baidu.oped.apm.profiler.context.DefaultServerMetaDataHolder;
import com.baidu.oped.apm.profiler.context.DefaultTraceContext;
import com.baidu.oped.apm.profiler.context.ThreadLocalTraceFactory;
import com.baidu.oped.apm.profiler.context.storage.LogStorageFactory;
import com.baidu.oped.apm.profiler.monitor.metric.MetricRegistry;
import com.baidu.oped.apm.profiler.sampler.TrueSampler;

import org.junit.Assert;
import org.junit.Test;

public class ThreadLocalTraceFactoryTest {

    private ThreadLocalTraceFactory getTraceFactory() {
        IdGenerator idGenerator = new IdGenerator();
        LogStorageFactory logStorageFactory = new LogStorageFactory();
        TrueSampler trueSampler = new TrueSampler();
        ServerMetaDataHolder serverMetaDataHolder = new DefaultServerMetaDataHolder(Collections.<String>emptyList());
        AgentInformation agentInformation = new AgentInformation("agentId", "applicationName", System.currentTimeMillis(), 10, "test", "127.0.0.1", ServiceType.STAND_ALONE,
                JvmUtils.getSystemProperty(SystemPropertyKey.JAVA_VERSION), Version.VERSION);
        DefaultTraceContext traceContext = new DefaultTraceContext(100, agentInformation, logStorageFactory, trueSampler, serverMetaDataHolder, false);
        return new ThreadLocalTraceFactory(traceContext, logStorageFactory, trueSampler, idGenerator);
    }

    @Test
    public void nullTraceObject() {
        ThreadLocalTraceFactory traceFactory = getTraceFactory();

        Trace currentTraceObject = traceFactory.currentTraceObject();
        Assert.assertNull(currentTraceObject);

        Trace rawTraceObject = traceFactory.currentRawTraceObject();
        Assert.assertNull(rawTraceObject);

    }

    @Test
    public void testCurrentTraceObject() throws Exception {
        ThreadLocalTraceFactory traceFactory = getTraceFactory();

        Trace trace = traceFactory.currentTraceObject();

    }

    @Test
    public void testCurrentRpcTraceObject() throws Exception {

    }

    @Test
    public void testCurrentRawTraceObject() throws Exception {

    }

    @Test
    public void testDisableSampling() throws Exception {

    }

    @Test
    public void testContinueTraceObject() throws Exception {

    }

    @Test
    public void testNewTraceObject() throws Exception {

    }

    @Test
    public void testDetachTraceObject() throws Exception {

    }
}