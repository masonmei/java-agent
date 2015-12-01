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
package com.baidu.oped.apm.profiler.objectfactory;

import java.lang.annotation.Annotation;

import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentMethod;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.Name;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.NoCache;
import com.baidu.oped.apm.bootstrap.interceptor.group.InterceptorGroup;
import com.baidu.oped.apm.exception.ApmException;
import com.baidu.oped.apm.profiler.util.TypeUtils;

/**
 * @author Jongho Moon
 *
 */
public class InterceptorArgumentProvider implements ArgumentProvider {
    private final TraceContext traceContext;
    private final InterceptorGroup interceptorGroup;
    private final InstrumentClass targetClass;
    private final InstrumentMethod targetMethod;

    public InterceptorArgumentProvider(TraceContext traceContext, InstrumentClass targetClass) {
        this(traceContext, null, targetClass, null);
    }
    
    public InterceptorArgumentProvider(TraceContext traceContext, InterceptorGroup interceptorGroup, InstrumentClass targetClass, InstrumentMethod targetMethod) {
        this.traceContext = traceContext;
        this.interceptorGroup = interceptorGroup;
        this.targetClass = targetClass;
        this.targetMethod = targetMethod;
    }

    @Override
    public Option get(int index, Class<?> type, Annotation[] annotations) {
        if (type == InstrumentClass.class) {
            return Option.withValue(targetClass);
        } else if (type == MethodDescriptor.class) {
            MethodDescriptor descriptor = targetMethod.getDescriptor();
            cacheApiIfAnnotationNotPresent(annotations, descriptor);
            
            return Option.withValue(descriptor);
        } else if (type == InstrumentMethod.class) {
            return Option.withValue(targetMethod);
        } else if (type == InterceptorGroup.class) {
            Name annotation = TypeUtils.findAnnotation(annotations, Name.class);
            
            if (annotation == null) {
                if (interceptorGroup == null) {
                    throw new ApmException("Group parameter is not annotated with @Name and the target class is not associated with any Group");
                } else {
                    return Option.withValue(interceptorGroup);
                }
            } else {
                return Option.empty();
            }
        }
        
        return Option.empty();
    }

    private void cacheApiIfAnnotationNotPresent(Annotation[] annotations, MethodDescriptor descriptor) {
        Annotation annotation = TypeUtils.findAnnotation(annotations, NoCache.class);
        if (annotation == null) {
            traceContext.cacheApi(descriptor);
        }
    }
}
