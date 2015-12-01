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
package com.baidu.oped.apm.plugin.google.httpclient.interceptor;

import com.baidu.oped.apm.bootstrap.async.AsyncTraceIdAccessor;
import com.baidu.oped.apm.bootstrap.context.AsyncTraceId;
import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.AroundInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.Group;
import com.baidu.oped.apm.bootstrap.interceptor.group.ExecutionPolicy;
import com.baidu.oped.apm.bootstrap.interceptor.group.InterceptorGroup;
import com.baidu.oped.apm.bootstrap.interceptor.group.InterceptorGroupInvocation;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.plugin.google.httpclient.HttpClientConstants;

/**
 * 
 * @author jaehong.kim
 *
 */
@Group(value = HttpClientConstants.EXECUTE_ASYNC_SCOPE, executionPolicy = ExecutionPolicy.ALWAYS)
public class HttpRequestExecuteAsyncMethodInnerClassConstructorInterceptor implements AroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private InterceptorGroup interceptorGroup;

    public HttpRequestExecuteAsyncMethodInnerClassConstructorInterceptor(TraceContext traceContext, MethodDescriptor descriptor, InterceptorGroup interceptorGroup) {
        this.interceptorGroup = interceptorGroup;
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        try {
            if (!validate(target, args)) {
                return;
            }

            final InterceptorGroupInvocation transaction = interceptorGroup.getCurrentInvocation();
            if (transaction != null && transaction.getAttachment() != null) {
                final AsyncTraceId asyncTraceId = (AsyncTraceId) transaction.getAttachment();
                ((AsyncTraceIdAccessor)target)._$APM$_setAsyncTraceId(asyncTraceId);
                // clear.
                transaction.removeAttachment();
            }
        } catch (Throwable t) {
            logger.warn("Failed to BEFORE process. {}", t.getMessage(), t);
        }
    }

    private boolean validate(final Object target, final Object[] args) {
        if (!(target instanceof AsyncTraceIdAccessor)) {
            logger.debug("Invalid target object. Need field accessor({}).", HttpClientConstants.METADATA_ASYNC_TRACE_ID);
            return false;
        }

        return true;
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args);
        }
    }
}