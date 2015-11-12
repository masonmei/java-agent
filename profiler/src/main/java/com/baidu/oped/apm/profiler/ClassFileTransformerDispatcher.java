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

package com.baidu.oped.apm.profiler;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.bootstrap.config.Filter;
import com.baidu.oped.apm.bootstrap.instrument.DynamicTransformRequestListener;
import com.baidu.oped.apm.profiler.instrument.LegacyProfilerPluginClassInjector;
import com.baidu.oped.apm.profiler.instrument.transformer.DebugTransformer;
import com.baidu.oped.apm.profiler.instrument.transformer.DefaultTransformerRegistry;
import com.baidu.oped.apm.profiler.instrument.transformer.TransformerRegistry;
import com.baidu.oped.apm.profiler.plugin.DefaultProfilerPluginContext;
import com.baidu.oped.apm.profiler.plugin.xml.transformer.MatchableClassFileTransformer;
import com.baidu.oped.apm.profiler.util.JavaAssistUtils;

/**
 * @author emeroad
 * @author netspider
 */
public class ClassFileTransformerDispatcher implements ClassFileTransformer, DynamicTransformRequestListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final ClassLoader agentClassLoader = this.getClass().getClassLoader();

    private final TransformerRegistry transformerRegistry;
    private final DynamicTransformerRegistry dynamicTransformerRegistry;
    
    private final DefaultProfilerPluginContext globalContext;    
    private final Filter<String> debugTargetFilter;
    private final DebugTransformer debugTransformer;

    private final ClassFileFilter unmodifiableFilter;
    
    public ClassFileTransformerDispatcher(DefaultAgent agent, List<DefaultProfilerPluginContext> pluginContexts) {
        if (agent == null) {
            throw new NullPointerException("agent must not be null");
        }
        
        this.globalContext = new DefaultProfilerPluginContext(agent, new LegacyProfilerPluginClassInjector(getClass().getClassLoader()));
        this.debugTargetFilter = agent.getProfilerConfig().getProfilableClassFilter();
        this.debugTransformer = new DebugTransformer(globalContext);

        this.unmodifiableFilter = new UnmodifiableClassFilter(agentClassLoader);
        
        this.transformerRegistry = createTransformerRegistry(pluginContexts);
        this.dynamicTransformerRegistry = new DefaultDynamicTransformerRegistry();
    }

    @Override
    public byte[] transform(ClassLoader classLoader, String jvmClassName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) throws IllegalClassFormatException {
        ClassFileTransformer transformer = dynamicTransformerRegistry.getTransformer(classLoader, jvmClassName);
        
        if (transformer != null) {
            return transform0(classLoader, jvmClassName, classBeingRedefined, protectionDomain, classFileBuffer, transformer);
        }
        
        if (!unmodifiableFilter.accept(classLoader, jvmClassName, classBeingRedefined, protectionDomain, classFileBuffer)) {
            return null;
        }

        transformer = this.transformerRegistry.findTransformer(jvmClassName);
        
        if (transformer == null) {
            // For debug
            // TODO What if a modifier is duplicated?
            if (this.debugTargetFilter.filter(jvmClassName)) {
                // Added to see if call stack view is OK on a test machine.
                transformer = debugTransformer;
            } else {
                return null;
            }
        }

        return transform0(classLoader, jvmClassName, classBeingRedefined, protectionDomain, classFileBuffer, transformer);
    }

    private byte[] transform0(ClassLoader classLoader, String jvmClassName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer, ClassFileTransformer transformer) {
        final String javaClassName = JavaAssistUtils.jvmNameToJavaName(jvmClassName);

        if (isDebug) {
            if (classBeingRedefined == null) {
                logger.debug("[transform] classLoader:{} className:{} trnasformer:{}", classLoader, javaClassName, transformer.getClass().getName());
            } else {
                logger.debug("[retransform] classLoader:{} className:{} trnasformer:{}", classLoader, javaClassName, transformer.getClass().getName());
            }
        }

        try {
            final Thread thread = Thread.currentThread();
            final ClassLoader before = getContextClassLoader(thread);
            thread.setContextClassLoader(this.agentClassLoader);
            try {
                return transformer.transform(classLoader, javaClassName, classBeingRedefined, protectionDomain, classFileBuffer);
            } finally {
                // The context class loader have to be recovered even if it was null.
                thread.setContextClassLoader(before);
            }
        } catch (Throwable e) {
            logger.error("Transformer:{} threw an exception. cl:{} ctxCl:{} agentCl:{} Cause:{}",
                    transformer.getClass().getName(), classLoader, Thread.currentThread().getContextClassLoader(), agentClassLoader, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void onRetransformRequest(Class<?> target, final ClassFileTransformer transformer) {
        this.dynamicTransformerRegistry.onRetransformRequest(target, transformer);
    }

    @Override
    public void onTransformRequest(ClassLoader classLoader, String targetClassName, ClassFileTransformer transformer) {
        this.dynamicTransformerRegistry.onTransformRequest(classLoader, targetClassName, transformer);
    }

    private ClassLoader getContextClassLoader(Thread thread) throws Throwable {
        try {
            return thread.getContextClassLoader();
        } catch (SecurityException se) {
            throw se;
        } catch (Throwable th) {
            if (isDebug) {
                logger.debug("getContextClassLoader(). Caused:{}", th.getMessage(), th);
            }
            throw th;
        }
    }

    private TransformerRegistry createTransformerRegistry(List<DefaultProfilerPluginContext> pluginContexts) {
        DefaultTransformerRegistry registry = new DefaultTransformerRegistry();

        for (DefaultProfilerPluginContext pluginContext : pluginContexts) {
            for (ClassFileTransformer transformer : pluginContext.getClassEditors()) {
                if (transformer instanceof MatchableClassFileTransformer) {
                    MatchableClassFileTransformer t = (MatchableClassFileTransformer)transformer;
                    logger.info("Registering class file transformer {} for {} ", t, t.getMatcher());
                    registry.addTransformer(t.getMatcher(), t);
                } else {
                    logger.warn("Ignore class file transformer {}", transformer);
                }
            }
        }
        
        return registry;
    }

}