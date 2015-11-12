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
package com.baidu.oped.apm.plugin.arcus.interceptor;

import com.baidu.oped.apm.bootstrap.context.AsyncTraceId;
import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.SpanEventRecorder;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.SpanAsyncEventSimpleAroundInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.Group;
import com.baidu.oped.apm.plugin.arcus.ArcusConstants;

/**
 * @author emeroad
 * @author jaehong.kim
 */
@Group(ArcusConstants.ARCUS_FUTURE_SCOPE)
public class FutureInternalMethodInterceptor extends SpanAsyncEventSimpleAroundInterceptor {

    public FutureInternalMethodInterceptor(MethodDescriptor methodDescriptor, TraceContext traceContext) {
        super(traceContext, methodDescriptor);
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, AsyncTraceId asyncTraceId, Object target, Object[] args) {
    }

    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        recorder.recordServiceType(ArcusConstants.ARCUS_INTERNAL);
        recorder.recordException(throwable);
        recorder.recordApi(methodDescriptor);
    }
}