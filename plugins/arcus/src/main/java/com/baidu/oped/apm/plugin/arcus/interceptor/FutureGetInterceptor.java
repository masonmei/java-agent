/**
 * Copyright 2014 NAVER Corp.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.oped.apm.plugin.arcus.interceptor;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ops.Operation;

import com.baidu.oped.apm.bootstrap.context.AsyncTraceId;
import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.SpanEventRecorder;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.SpanAsyncEventSimpleAroundInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.Group;
import com.baidu.oped.apm.plugin.arcus.ArcusConstants;
import com.baidu.oped.apm.plugin.arcus.OperationAccessor;
import com.baidu.oped.apm.plugin.arcus.ServiceCodeAccessor;

/**
 * @author emeroad
 * @author jaehong.kim
 */
@Group(ArcusConstants.ARCUS_FUTURE_SCOPE)
public class FutureGetInterceptor extends SpanAsyncEventSimpleAroundInterceptor {

    public FutureGetInterceptor(MethodDescriptor methodDescriptor, TraceContext traceContext) {
        super(traceContext, methodDescriptor);
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, AsyncTraceId asyncTraceId, Object target, Object[] args) {
    }

    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        recorder.recordApi(methodDescriptor);
        recorder.recordDestinationId("MEMCACHED");
        recorder.recordServiceType(ArcusConstants.MEMCACHED_FUTURE_GET);

        if (!(target instanceof OperationAccessor)) {
            logger.info("operation not found");
            return;
        }

        // find the target node
        final Operation op = ((OperationAccessor) target)._$PINPOINT$_getOperation();
        if (op == null) {
            logger.info("operation is null");
            return;
        }

        recorder.recordException(op.getException());
        MemcachedNode handlingNode = op.getHandlingNode();
        if (handlingNode != null) {
            SocketAddress socketAddress = handlingNode.getSocketAddress();
            if (socketAddress instanceof InetSocketAddress) {
                InetSocketAddress address = (InetSocketAddress) socketAddress;
                recorder.recordEndPoint(address.getHostName() + ":" + address.getPort());
            }
        } else {
            logger.info("no handling node");
        }

        if (op instanceof ServiceCodeAccessor) {
            // determine the service type
            String serviceCode = ((ServiceCodeAccessor) op)._$PINPOINT$_getServiceCode();
            if (serviceCode != null) {
                recorder.recordDestinationId(serviceCode);
                recorder.recordServiceType(ArcusConstants.ARCUS_FUTURE_GET);
            }
        }
    }
}