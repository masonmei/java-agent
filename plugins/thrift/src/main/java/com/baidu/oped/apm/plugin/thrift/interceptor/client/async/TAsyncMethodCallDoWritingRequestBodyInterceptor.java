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

package com.baidu.oped.apm.plugin.thrift.interceptor.client.async;

import com.baidu.oped.apm.plugin.thrift.ThriftConstants;
import org.apache.thrift.async.TAsyncMethodCall;

import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.SpanEventRecorder;
import com.baidu.oped.apm.bootstrap.context.Trace;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.plugin.thrift.ThriftUtils;
import com.baidu.oped.apm.plugin.thrift.field.accessor.AsyncCallEndFlagFieldAccessor;
import com.baidu.oped.apm.plugin.thrift.field.accessor.AsyncCallRemoteAddressFieldAccessor;
import com.baidu.oped.apm.plugin.thrift.field.accessor.AsyncNextSpanIdFieldAccessor;

/**
 * @author HyunGil Jeong
 */
public class TAsyncMethodCallDoWritingRequestBodyInterceptor extends TAsyncMethodCallInternalMethodInterceptor {

    public TAsyncMethodCallDoWritingRequestBodyInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor) {
        super(traceContext, methodDescriptor);
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, Object target, Object[] args) {
        super.doInBeforeTrace(recorder, target, args);

        Long nextSpanId = ((AsyncNextSpanIdFieldAccessor)target)._$APM$_getAsyncNextSpanId();
        recorder.recordNextSpanId(nextSpanId);

        String remoteAddress = ((AsyncCallRemoteAddressFieldAccessor)target)._$APM$_getAsyncCallRemoteAddress();
        recorder.recordDestinationId(remoteAddress);

        String methodUri = ThriftUtils.getAsyncMethodCallName((TAsyncMethodCall<?>)target);
        String thriftUrl = remoteAddress + "/" + methodUri;
        recorder.recordAttribute(ThriftConstants.THRIFT_URL, thriftUrl);
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        super.after(target, args, result, throwable);

        // End async trace block if TAsyncMethodCall.cleanUpAndFireCallback(...) call completed successfully
        // if there was an exception, TAsyncMethodCall.onError(...) will be called and the async trace block will be ended there
        if (throwable != null) {
            return;
        }
        boolean endAsyncBlock = ((AsyncCallEndFlagFieldAccessor)target)._$APM$_getAsyncCallEndFlag();
        if (endAsyncBlock) {
            final Trace trace = super.traceContext.currentTraceObject();
            // shouldn't be null
            if (trace == null) {
                return;
            }

            if (trace.isAsync() && trace.isRootStack()) {
                trace.close();
                super.traceContext.removeTraceObject();
            }
        }
    }

    @Override
    protected boolean validate(Object target) {
        if (!(target instanceof AsyncNextSpanIdFieldAccessor)) {
            if (isDebug) {
                logger.debug("Invalid target object. Need field accessor({}).", AsyncNextSpanIdFieldAccessor.class.getName());
            }
            return false;
        }
        if (!(target instanceof AsyncCallRemoteAddressFieldAccessor)) {
            if (isDebug) {
                logger.debug("Invalid target object. Need field accessor({}).", AsyncCallRemoteAddressFieldAccessor.class.getName());
            }
            return false;
        }
        if (!(target instanceof AsyncCallEndFlagFieldAccessor)) {
            if (isDebug) {
                logger.debug("Invalid target object. Need field accessor({}).", AsyncCallEndFlagFieldAccessor.class.getName());
            }
            return false;
        }
        return super.validate(target);
    }

    @Override
    protected ServiceType getServiceType() {
        return ThriftConstants.THRIFT_CLIENT;
    }
}