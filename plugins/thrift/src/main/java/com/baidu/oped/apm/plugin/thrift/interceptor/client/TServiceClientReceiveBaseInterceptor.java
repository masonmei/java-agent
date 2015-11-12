/*
 * Copyright 2015 NAVER Corp.
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

package com.baidu.oped.apm.plugin.thrift.interceptor.client;

import org.apache.thrift.TBase;

import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.SpanEventRecorder;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.SpanEventSimpleAroundInterceptorForPlugin;
import com.baidu.oped.apm.bootstrap.util.StringUtils;
import com.baidu.oped.apm.plugin.thrift.ThriftConstants;

/**
 * This interceptor records the response received from the server for synchronous client calls.
 * <p>
 * Based on Thrift 0.8.0+
 * 
 * @author HyunGil Jeong
 */
public class TServiceClientReceiveBaseInterceptor extends SpanEventSimpleAroundInterceptorForPlugin {

    private final boolean traceServiceResult;
    
    public TServiceClientReceiveBaseInterceptor(TraceContext context, MethodDescriptor descriptor, boolean traceServiceResult) {
        super(context, descriptor);
        this.traceServiceResult = traceServiceResult;
    }
    
    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, Object target, Object[] args) {
        recorder.recordServiceType(ThriftConstants.THRIFT_CLIENT_INTERNAL);
    }
    
    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        recorder.recordApi(getMethodDescriptor());
        if (throwable == null && this.traceServiceResult) {
            if (args.length == 2 && (args[0] instanceof TBase)) {
                String resultString = getResult((TBase<?, ?>)args[0]);
                recorder.recordAttribute(ThriftConstants.THRIFT_RESULT, resultString);
            }
        } else {
            recorder.recordException(throwable);
        }
    }

    private String getResult(TBase<?, ?> args) {
        return StringUtils.drop(args.toString(), 256);
    }
}