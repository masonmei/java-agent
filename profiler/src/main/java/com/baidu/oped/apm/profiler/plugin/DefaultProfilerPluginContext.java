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

package com.baidu.oped.apm.profiler.plugin;

import java.lang.instrument.ClassFileTransformer;
import java.util.ArrayList;
import java.util.List;

import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.NotFoundInstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.Instrumentor;
import com.baidu.oped.apm.bootstrap.instrument.matcher.Matcher;
import com.baidu.oped.apm.bootstrap.instrument.matcher.Matchers;
import com.baidu.oped.apm.bootstrap.instrument.transformer.TransformCallback;
import com.baidu.oped.apm.bootstrap.interceptor.group.InterceptorGroup;
import com.baidu.oped.apm.bootstrap.plugin.ApplicationTypeDetector;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPluginSetupContext;
import com.baidu.oped.apm.profiler.DefaultAgent;
import com.baidu.oped.apm.profiler.DynamicTransformService;
import com.baidu.oped.apm.profiler.instrument.ClassInjector;
import com.baidu.oped.apm.profiler.interceptor.group.DefaultInterceptorGroup;
import com.baidu.oped.apm.profiler.util.JavaAssistUtils;
import com.baidu.oped.apm.profiler.util.NameValueList;

public class DefaultProfilerPluginContext implements ProfilerPluginSetupContext, Instrumentor {
    private final DefaultAgent agent;
    private final ClassInjector classInjector;
    
    private final List<ApplicationTypeDetector> serverTypeDetectors = new ArrayList<ApplicationTypeDetector>();
    private final List<ClassFileTransformer> classTransformers = new ArrayList<ClassFileTransformer>();
    
    private final NameValueList<InterceptorGroup> interceptorGroups = new NameValueList<InterceptorGroup>();
    
    public DefaultProfilerPluginContext(DefaultAgent agent, ClassInjector classInjector) {
        if (agent == null) {
            throw new NullPointerException("agent must not be null");
        }
        if (classInjector == null) {
            throw new NullPointerException("classInjector must not be null");
        }
        this.agent = agent;
        this.classInjector = classInjector;
    }

    @Override
    public ProfilerConfig getConfig() {
        return agent.getProfilerConfig();
    }

    @Override
    public TraceContext getTraceContext() {
        final TraceContext context = agent.getTraceContext();
        if (context == null) {
            throw new IllegalStateException("TraceContext is not created yet");
        }
        
        return context;
    }
        
    @Override
    public void addApplicationTypeDetector(ApplicationTypeDetector... detectors) {
        if (detectors == null) {
            return;
        }
        for (ApplicationTypeDetector detector : detectors) {
            serverTypeDetectors.add(detector);
        }
    }
    
    @Override
    public InstrumentClass getInstrumentClass(ClassLoader classLoader, String className, byte[] classFileBuffer) {
        if (className == null) {
            throw new NullPointerException("className must not be null");
        }
        try {
            return agent.getClassPool().getClass(this, classLoader, className, classFileBuffer);
        } catch (NotFoundInstrumentException e) {
            return null;
        }
    }
    
    @Override
    public boolean exist(ClassLoader classLoader, String className) {
        if (className == null) {
            throw new NullPointerException("className must not be null");
        }

        return agent.getClassPool().hasClass(classLoader, className);
    }

    @Override
    public void addClassFileTransformer(final String targetClassName, final TransformCallback transformCallback) {
        if (targetClassName == null) {
            throw new NullPointerException("targetClassName must not be null");
        }
        if (transformCallback == null) {
            throw new NullPointerException("transformCallback must not be null");
        }

        final Matcher matcher = Matchers.newClassNameMatcher(JavaAssistUtils.javaNameToJvmName(targetClassName));
        final MatchableClassFileTransformerGuardDelegate guard = new MatchableClassFileTransformerGuardDelegate(this, matcher, transformCallback);
        classTransformers.add(guard);
    }
    
    @Override
    public void addClassFileTransformer(ClassLoader classLoader, String targetClassName, final TransformCallback transformCallback) {
        if (targetClassName == null) {
            throw new NullPointerException("targetClassName must not be null");
        }
        if (transformCallback == null) {
            throw new NullPointerException("transformCallback must not be null");
        }

        final ClassFileTransformerGuardDelegate classFileTransformerGuardDelegate = new ClassFileTransformerGuardDelegate(this, transformCallback);

        final DynamicTransformService dynamicTransformService = agent.getDynamicTransformService();
        dynamicTransformService.addClassFileTransformer(classLoader, targetClassName, classFileTransformerGuardDelegate);
    }


    @Override
    public void retransform(Class<?> target, final TransformCallback transformCallback) {
        if (target == null) {
            throw new NullPointerException("target must not be null");
        }
        if (transformCallback == null) {
            throw new NullPointerException("transformCallback must not be null");
        }

        final ClassFileTransformerGuardDelegate classFileTransformerGuardDelegate = new ClassFileTransformerGuardDelegate(this, transformCallback);

        final DynamicTransformService dynamicTransformService = agent.getDynamicTransformService();
        dynamicTransformService.retransform(target, classFileTransformerGuardDelegate);
    }


    @Override
    public <T> Class<? extends T> injectClass(ClassLoader targetClassLoader, String className) {
        if (className == null) {
            throw new NullPointerException("className must not be null");
        }

        return classInjector.injectClass(targetClassLoader, className);
    }

    public List<ClassFileTransformer> getClassEditors() {
        return classTransformers;
    }

    public List<ApplicationTypeDetector> getApplicationTypeDetectors() {
        return serverTypeDetectors;
    }

    @Override
    public InterceptorGroup getInterceptorGroup(String name) {
        if (name == null) {
            throw new NullPointerException("name must not be null");
        }
        InterceptorGroup group = interceptorGroups.get(name);
        
        if (group == null) {
            group = new DefaultInterceptorGroup(name);
            interceptorGroups.add(name, group);
        }
        
        return group;
    }
}
