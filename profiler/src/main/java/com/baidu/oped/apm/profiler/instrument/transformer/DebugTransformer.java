/**
 * Copyright 2014 NAVER Corp.
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
package com.baidu.oped.apm.profiler.instrument.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentMethod;
import com.baidu.oped.apm.bootstrap.instrument.MethodFilters;
import com.baidu.oped.apm.bootstrap.interceptor.BasicMethodInterceptor;
import com.baidu.oped.apm.profiler.plugin.DefaultProfilerPluginContext;

/**
 * @author Jongho Moon
 *
 */
public class DebugTransformer implements ClassFileTransformer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DefaultProfilerPluginContext context;
    
    public DebugTransformer(DefaultProfilerPluginContext context) {
        this.context = context;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            InstrumentClass target = context.getInstrumentClass(loader, className, classfileBuffer);
            
            if (!target.isInterceptable()) {
                return null;
            }
    
            for (InstrumentMethod method : target.getDeclaredMethods(MethodFilters.ACCEPT_ALL)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("### c={}, m={}, params={}", className, method.getName(), Arrays.toString(method.getParameterTypes()));
                }
                
                target.addInterceptor(BasicMethodInterceptor.class.getName());
            }
    
            return target.toBytecode();
        } catch (InstrumentException e) {
            logger.warn("Failed to instrument " + className, e);
            return null;
        }
    }
    
    
}
