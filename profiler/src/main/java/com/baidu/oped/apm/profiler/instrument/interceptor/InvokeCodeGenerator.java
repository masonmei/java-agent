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
package com.baidu.oped.apm.profiler.instrument.interceptor;

import java.lang.reflect.Modifier;

import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentMethod;
import com.baidu.oped.apm.bootstrap.interceptor.AfterInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.ApiIdAwareAroundInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.BeforeInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.InterceptorInvokerHelper;
import com.baidu.oped.apm.bootstrap.interceptor.StaticAroundInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.registry.InterceptorRegistry;
import com.baidu.oped.apm.profiler.util.JavaAssistUtils;

/**
 * @author Jongho Moon
 *
 */
public class InvokeCodeGenerator {
    private final TraceContext traceContext;
    protected final Class<?> interceptorClass;
    protected final InstrumentMethod targetMethod;
    protected final int interceptorId;
    protected final Type type;
    
    public InvokeCodeGenerator(int interceptorId, Class<?> interceptorClass, InstrumentMethod targetMethod, TraceContext traceContext) {
        this.interceptorClass = interceptorClass;
        this.targetMethod = targetMethod;
        this.interceptorId = interceptorId;
        this.traceContext = traceContext;
        
        if (BeforeInterceptor.class.isAssignableFrom(interceptorClass) || AfterInterceptor.class.isAssignableFrom(interceptorClass)) {
            type = Type.ARRAY_ARGS;
        } else if (StaticAroundInterceptor.class.isAssignableFrom(interceptorClass)) {
            type = Type.STATIC;
        } else if (ApiIdAwareAroundInterceptor.class.isAssignableFrom(interceptorClass)) {
            type = Type.API_ID_AWARE;
        } else {
            type = Type.BASIC;
        }
    }

    protected enum Type {
        ARRAY_ARGS, STATIC, BASIC, API_ID_AWARE
    }

    protected String getInterceptorType() {
        return interceptorClass.getName();
    }

    protected String getParameterTypes() {
        String[] parameterTypes = targetMethod.getParameterTypes();
        return JavaAssistUtils.getParameterDescription(parameterTypes);
    }

    protected String getTarget() {
        return Modifier.isStatic(targetMethod.getModifiers()) ? "null" : "this";
    }

    protected String getArguments() {
        if (targetMethod.getParameterTypes().length == 0) {
            return "null";
        }

        return "$args";
    }
    
    protected int getApiId() {
        MethodDescriptor descriptor = targetMethod.getDescriptor();
        int apiId = traceContext.cacheApi(descriptor);
        return apiId;
    }
    
    protected String getInterceptorInvokerHelperClassName() {
        return InterceptorInvokerHelper.class.getName();
    }

    protected String getInterceptorRegistryClassName() {
        return InterceptorRegistry.class.getName();
    }
    
    protected String getInterceptorVar() {
        return getInterceptorVar(interceptorId);
    }
    
    public static String getInterceptorVar(int interceptorId) {
        return "_$PINPOINT$_interceptor" + interceptorId;
    }
}