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

import org.apache.http.*;

/**
 * Trace status code.
 * 
 * @author minwoo.jung
 * @author jaehong.kim
 */
@Group(value = HttpClient4Constants.HTTP_CLIENT4_SCOPE, executionPolicy = ExecutionPolicy.INTERNAL)
public class HttpClientExecuteMethodInternalInterceptor implements AroundInterceptor {

    private boolean isHasCallbackParam;

    protected final PLogger logger;
    protected final boolean isDebug;

    protected final TraceContext traceContext;
    private final InterceptorGroup interceptorGroup;

    public HttpClientExecuteMethodInternalInterceptor(boolean isHasCallbackParam, TraceContext context, InterceptorGroup interceptorGroup) {
        this.logger = PLoggerFactory.getLogger(this.getClass());
        this.isDebug = logger.isDebugEnabled();

        this.traceContext = context;
        this.interceptorGroup = interceptorGroup;
        this.isHasCallbackParam = isHasCallbackParam;
    }

    @Override
    public void before(Object target, Object[] args) {
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, target.getClass().getName(), "", "internal", args);
        }

        if (!needGetStatusCode()) {
            return;
        }

        if (result instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) result;

            if (response.getStatusLine() != null) {
                HttpCallContext context = new HttpCallContext();
                final StatusLine statusLine = response.getStatusLine();
                if (statusLine != null) {
                    context.setStatusCode(statusLine.getStatusCode());
                    InterceptorGroupInvocation transaction = interceptorGroup.getCurrentInvocation();
                    transaction.setAttachment(context);
                }
            }
        }
    }

    private boolean needGetStatusCode() {
        if (isHasCallbackParam) {
            return false;
        }

        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return false;
        }

        // TODO fix me.
//        if (trace.getServiceType() != ServiceType.ASYNC_HTTP_CLIENT.getCode()) {
//            return false;
//        }

        InterceptorGroupInvocation transaction = interceptorGroup.getCurrentInvocation();
        if (transaction.getAttachment() != null) {
            return false;
        }

        return true;
    }
}
