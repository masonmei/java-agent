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
package com.baidu.oped.apm.plugin.spring.beans;

import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentMethod;
import com.baidu.oped.apm.bootstrap.instrument.Instrumentor;
import com.baidu.oped.apm.bootstrap.instrument.MethodFilter;
import com.baidu.oped.apm.bootstrap.instrument.MethodFilters;
import com.baidu.oped.apm.bootstrap.instrument.transformer.TransformCallback;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;

/**
 * @author Jongho Moon
 *
 */
public class BeanMethodTransformer implements TransformCallback {
    private static final int REQUIRED_ACCESS_FLAG = Modifier.PUBLIC;
    private static final int REJECTED_ACCESS_FLAG = Modifier.ABSTRACT |  Modifier.NATIVE | Modifier.STATIC;
    private static final MethodFilter METHOD_FILTER = MethodFilters.modifier(REQUIRED_ACCESS_FLAG, REJECTED_ACCESS_FLAG);

    private final PLogger logger = PLoggerFactory.getLogger(getClass());
    
    private AtomicInteger interceptorId = new AtomicInteger(-1);
    
    
    @Override
    public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
        if (logger.isInfoEnabled()) {
            logger.info("Modify {}", className);
        }

        try {
            InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);

            if (!target.isInterceptable()) {
                return null;
            }

            List<InstrumentMethod> methodList = target.getDeclaredMethods(METHOD_FILTER);
            for (InstrumentMethod method : methodList) {
                if (logger.isTraceEnabled()) {
                    logger.trace("### c={}, m={}, params={}", new Object[] {className, method.getName(), Arrays.toString(method.getParameterTypes())});
                }

                addInterceptor(method);
            }

            return target.toBytecode();
        } catch (Exception e) {
            logger.warn("modify fail. Cause:{}", e.getMessage(), e);
            return null;
        }
    }
    
    private void addInterceptor(InstrumentMethod targetMethod) throws InstrumentException {
        int id = interceptorId.get();
        
        if (id != -1) {
            targetMethod.addInterceptor(id);
            return;
        }
        
        synchronized (interceptorId) {
            id = interceptorId.get();
            
            if (id != -1) {
                targetMethod.addInterceptor(id);
                return;
            }
            
            id = targetMethod.addInterceptor("com.baidu.oped.apm.plugin.spring.beans.interceptor.BeanMethodInterceptor");
            interceptorId.set(id);
        }
    }
}
