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

package com.baidu.oped.apm.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.baidu.oped.apm.bootstrap.config.DefaultProfilerConfig;
import com.baidu.oped.apm.profiler.plugin.GuardProfilerPluginContext;
import org.apache.thrift.TBase;

import com.baidu.oped.apm.bootstrap.AgentOption;
import com.baidu.oped.apm.bootstrap.DefaultAgentOption;
import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;
import com.baidu.oped.apm.bootstrap.context.ServerMetaDataHolder;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPlugin;
import com.baidu.oped.apm.bootstrap.plugin.test.ExpectedAnnotation;
import com.baidu.oped.apm.common.plugin.PluginLoader;
import com.baidu.oped.apm.common.service.DefaultAnnotationKeyRegistryService;
import com.baidu.oped.apm.common.service.DefaultServiceTypeRegistryService;
import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.profiler.DefaultAgent;
import com.baidu.oped.apm.profiler.context.Span;
import com.baidu.oped.apm.profiler.context.SpanEvent;
import com.baidu.oped.apm.profiler.context.storage.StorageFactory;
import com.baidu.oped.apm.profiler.instrument.ClassInjector;
import com.baidu.oped.apm.profiler.interceptor.registry.InterceptorRegistryBinder;
import com.baidu.oped.apm.profiler.plugin.DefaultProfilerPluginContext;
import com.baidu.oped.apm.profiler.receiver.CommandDispatcher;
import com.baidu.oped.apm.profiler.sender.DataSender;
import com.baidu.oped.apm.profiler.sender.EnhancedDataSender;
import com.baidu.oped.apm.profiler.util.RuntimeMXBeanUtils;
import com.baidu.oped.apm.thrift.dto.TAnnotation;

/**
 * @author emeroad
 * @author koo.taejin
 * @author hyungil.jeong
 */
public class MockAgent extends DefaultAgent {
    
    public static MockAgent of(String configPath) {
        ProfilerConfig profilerConfig = null;
        try {
            URL resource = MockAgent.class.getClassLoader().getResource(configPath);
            if (resource == null) {
                throw new FileNotFoundException("apm.config not found. configPath:" + configPath);
            }
            profilerConfig = DefaultProfilerConfig.load(resource.getPath());
            profilerConfig.setApplicationServerType(ServiceType.TEST_STAND_ALONE.getName());
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        
        return of(profilerConfig);
    }
    
    public static MockAgent of(ProfilerConfig config) {
        AgentOption agentOption = new DefaultAgentOption("", new DummyInstrumentation(), config, new URL[0], null, new DefaultServiceTypeRegistryService(), new DefaultAnnotationKeyRegistryService());
        InterceptorRegistryBinder binder = new TestInterceptorRegistryBinder();
        
        return new MockAgent(agentOption, binder);
    }

    public MockAgent(AgentOption agentOption) {
        this(agentOption, new TestInterceptorRegistryBinder());
    }
    
    public MockAgent(AgentOption agentOption, InterceptorRegistryBinder binder) {
        super(agentOption, binder);
    }

    @Override
    protected DataSender createUdpStatDataSender(int port, String threadName, int writeQueueSize, int timeout, int sendBufferSize) {
        return new ListenableDataSender<TBase<?, ?>>();
    }

    @Override
    protected DataSender createUdpSpanDataSender(int port, String threadName, int writeQueueSize, int timeout, int sendBufferSize) {
        return new ListenableDataSender<TBase<?, ?>>();
    }

    public DataSender getSpanDataSender() {
        return super.getSpanDataSender();
    }


    @Override
    protected StorageFactory createStorageFactory() {
        return new SimpleSpanStorageFactory(super.getSpanDataSender());
    }


    @Override
    protected EnhancedDataSender createTcpDataSender(CommandDispatcher commandDispatcher) {
        return new TestTcpDataSender();
    }

    @Override
    protected ServerMetaDataHolder createServerMetaDataHolder() {
        List<String> vmArgs = RuntimeMXBeanUtils.getVmArgs();
        return new ResettableServerMetaDataHolder(vmArgs);
    }
    
    @Override
    protected List<DefaultProfilerPluginContext> loadPlugins(AgentOption agentOption) {
        List<DefaultProfilerPluginContext> pluginContexts = new ArrayList<DefaultProfilerPluginContext>();
        ClassInjector classInjector = new TestProfilerPluginClassLoader();

        List<ProfilerPlugin> plugins = PluginLoader.load(ProfilerPlugin.class, ClassLoader.getSystemClassLoader());
        
        for (ProfilerPlugin plugin : plugins) {
            final DefaultProfilerPluginContext context = new DefaultProfilerPluginContext(this, classInjector);
            final GuardProfilerPluginContext guard = new GuardProfilerPluginContext(context);
            try {
                plugin.setup(guard);
            } finally {
                guard.close();
            }
            pluginContexts.add(context);
        }
        
        
        return pluginContexts;

    }

    public static String toString(Span span) {
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        builder.append(span.getServiceType());
        builder.append(", [");
        appendAnnotations(builder, span.getAnnotations());
        builder.append("])");
        
        return builder.toString();
    }

    public static String toString(SpanEvent span) {
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        builder.append(span.getServiceType());
        builder.append(", [");
        appendAnnotations(builder, span.getAnnotations());
        builder.append("])");
        
        return builder.toString();
    }

    private static void appendAnnotations(StringBuilder builder, List<TAnnotation> annotations) {
        boolean first = true;
        
        if (annotations != null) {
            for (TAnnotation a : annotations) {
                if (first) {
                    first = false;
                } else {
                    builder.append(", ");
                }
                
                builder.append(toString(a));
            }
        }
    }

    private static String toString(TAnnotation a) {
        return a.getKey() + "=" + a.getValue().getFieldValue();
    }
    
    public static String toString(short serviceCode, ExpectedAnnotation...annotations) {
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        builder.append(serviceCode);
        builder.append(", ");
        builder.append(Arrays.deepToString(annotations));
        builder.append(")");
        
        return builder.toString();
    }
}
