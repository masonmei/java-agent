/**
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
package com.baidu.oped.apm.plugin.json_lib.interceptor;

import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.SpanEventRecorder;
import com.baidu.oped.apm.bootstrap.context.Trace;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.AroundInterceptor;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.plugin.json_lib.JsonLibConstants;

/**
 * JsonLib method interceptor
 *
 * @author Sangyoon Lee
 */
public class ParsingInterceptor implements AroundInterceptor {
    private final TraceContext traceContext;
    private final MethodDescriptor descriptor;
    private final PLogger logger = PLoggerFactory.getLogger(getClass());

    public ParsingInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        this.traceContext = traceContext;
        this.descriptor = descriptor;
    }

    @Override
    public void before(Object target, Object[] args) {
        if (logger.isDebugEnabled()) {
            logger.beforeInterceptor(target, args);
        }

        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        trace.traceBlockBegin();
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (logger.isDebugEnabled()) {
            logger.afterInterceptor(target, args, result, throwable);
        }

        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        try {
            SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            recorder.recordServiceType(JsonLibConstants.SERVICE_TYPE);
            recorder.recordApi(descriptor);
            recorder.recordException(throwable);
            
            if (args.length > 0 && args[0] instanceof String) {
                recorder.recordAttribute(JsonLibConstants.JSON_LIB_ANNOTATION_KEY_JSON_LENGTH, ((String) args[0]).length());
            }
        } finally {
            trace.traceBlockEnd();
        }
    }
}
