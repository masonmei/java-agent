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

package com.baidu.oped.apm.plugin.user.interceptor;

import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.SpanEventRecorder;
import com.baidu.oped.apm.bootstrap.context.SpanRecorder;
import com.baidu.oped.apm.bootstrap.context.Trace;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.context.TraceType;
import com.baidu.oped.apm.bootstrap.interceptor.AroundInterceptor;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.plugin.user.UserConstants;
import com.baidu.oped.apm.plugin.user.UserIncludeMethodDescriptor;

/**
 * @author jaehong.kim
 */
public class UserIncludeMethodInterceptor implements AroundInterceptor {
    private static final UserIncludeMethodDescriptor USER_INCLUDE_METHOD_DESCRIPTOR = new UserIncludeMethodDescriptor();
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private TraceContext traceContext;
    private MethodDescriptor descriptor;

    public UserIncludeMethodInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor) {
        this.traceContext = traceContext;
        this.descriptor = methodDescriptor;

        traceContext.cacheApi(USER_INCLUDE_METHOD_DESCRIPTOR);
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            trace = traceContext.newTraceObject(TraceType.USER);
            if (!trace.canSampled()) {
                if(isDebug) {
                    logger.debug("New trace and can't sampled {}", trace);
                }
                return;
            } 
            if(isDebug) {
                logger.debug("New trace and sampled {}", trace);
            }
            SpanRecorder recorder = trace.getSpanRecorder();
            recordRootSpan(recorder);
        }

        trace.traceBlockBegin();
    }

    private void recordRootSpan(final SpanRecorder recorder) {
        // root
        recorder.recordServiceType(ServiceType.STAND_ALONE);
        recorder.recordApi(USER_INCLUDE_METHOD_DESCRIPTOR);
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args);
        }

        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        try {
            SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            recorder.recordApi(descriptor);
            recorder.recordServiceType(UserConstants.USER_INCLUDE);
            recorder.recordException(throwable);
        } finally {
            trace.traceBlockEnd();
            if(isDebug) {
                logger.debug("Closed user trace. {}", trace.getCallStackFrameId());
            }
            if(trace.getTraceType() == TraceType.USER && trace.isRootStack()) {
                if(isDebug) {
                    logger.debug("Closed user trace. {}", trace);
                }
                trace.close();
                traceContext.removeTraceObject();
            }
        }
    }
}