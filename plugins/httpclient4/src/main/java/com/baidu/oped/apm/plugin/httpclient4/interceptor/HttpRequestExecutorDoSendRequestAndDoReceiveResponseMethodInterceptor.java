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

package com.baidu.oped.apm.plugin.httpclient4.interceptor;

import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.Trace;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.AroundInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.Group;
import com.baidu.oped.apm.bootstrap.interceptor.group.ExecutionPolicy;
import com.baidu.oped.apm.bootstrap.interceptor.group.InterceptorGroup;
import com.baidu.oped.apm.bootstrap.interceptor.group.InterceptorGroupInvocation;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.plugin.httpclient4.HttpCallContext;
import com.baidu.oped.apm.plugin.httpclient4.HttpClient4Constants;

/**
 * @author jaehong.kim
 */
@Group(value=HttpClient4Constants.HTTP_CLIENT4_SCOPE, executionPolicy=ExecutionPolicy.ALWAYS)
public class HttpRequestExecutorDoSendRequestAndDoReceiveResponseMethodInterceptor implements AroundInterceptor {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final TraceContext traceContext;
    private final MethodDescriptor methodDescriptor;
    private final InterceptorGroup interceptorGroup;


    public HttpRequestExecutorDoSendRequestAndDoReceiveResponseMethodInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor, InterceptorGroup interceptorGroup) {
        this.traceContext = traceContext;
        this.methodDescriptor = methodDescriptor;
        this.interceptorGroup = interceptorGroup;
    }
    
    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, methodDescriptor.getClassName(), methodDescriptor.getMethodName(), "", args);
        }

        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        InterceptorGroupInvocation invocation = interceptorGroup.getCurrentInvocation();
        if(invocation != null && invocation.getAttachment() != null) {
            HttpCallContext callContext = (HttpCallContext) invocation.getAttachment();
            if(methodDescriptor.getMethodName().equals("doSendRequest")) {
                callContext.setWriteBeginTime(System.currentTimeMillis());
            } else {
                callContext.setReadBeginTime(System.currentTimeMillis());
            }
            logger.debug("Set call context {}", callContext);
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, methodDescriptor.getClassName(), methodDescriptor.getMethodName(), "", args, result, throwable);
        }

        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        InterceptorGroupInvocation invocation = interceptorGroup.getCurrentInvocation();
        if(invocation != null && invocation.getAttachment() != null) {
            HttpCallContext callContext = (HttpCallContext) invocation.getAttachment();
            if(methodDescriptor.getMethodName().equals("doSendRequest")) {
                callContext.setWriteEndTime(System.currentTimeMillis());
                callContext.setWriteFail(throwable != null);
            } else {
                callContext.setReadEndTime(System.currentTimeMillis());
                callContext.setReadFail(throwable != null);
            }
            logger.debug("Set call context {}", callContext);
        }
    }
}
