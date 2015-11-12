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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Future;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.ops.Operation;

import com.baidu.oped.apm.bootstrap.async.AsyncTraceIdAccessor;
import com.baidu.oped.apm.bootstrap.context.AsyncTraceId;
import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.SpanEventRecorder;
import com.baidu.oped.apm.bootstrap.context.Trace;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.AroundInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.Group;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.plugin.arcus.ArcusConstants;
import com.baidu.oped.apm.plugin.arcus.OperationAccessor;
import com.baidu.oped.apm.plugin.arcus.ServiceCodeAccessor;

/**
 * @author emeroad
 * @author jaehong.kim
 */
@Group(ArcusConstants.ARCUS_SCOPE)
public class ApiInterceptor implements AroundInterceptor {
    protected final PLogger logger = PLoggerFactory.getLogger(getClass());
    protected final boolean isDebug = logger.isDebugEnabled();

    protected final MethodDescriptor methodDescriptor;
    protected final TraceContext traceContext;

    private final boolean traceKey;
    private final int keyIndex;

    public ApiInterceptor(TraceContext context, MethodDescriptor targetMethod, boolean traceKey) {
        this.traceContext = context;
        this.methodDescriptor = targetMethod;

        if (traceKey) {
            int index = findFirstString(targetMethod);

            if (index != -1) {
                this.traceKey = true;
                this.keyIndex = index;
            } else {
                this.traceKey = false;
                this.keyIndex = -1;
            }
        } else {
            this.traceKey = false;
            this.keyIndex = -1;
        }
    }

    private static int findFirstString(MethodDescriptor method) {
        if (method == null) {
            return -1;
        }
        final String[] methodParams = method.getParameterTypes();
        final int minIndex = Math.min(methodParams.length, 3);
        for (int i = 0; i < minIndex; i++) {
            if ("java.lang.String".equals(methodParams[i])) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        try {
            trace.traceBlockBegin();
        } catch (Throwable th) {
            if (logger.isWarnEnabled()) {
                logger.warn("BEFORE. Caused:{}", th.getMessage(), th);
            }
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }

        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }
        try {
            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            if (traceKey) {
                final Object recordObject = args[keyIndex];
                recorder.recordApi(methodDescriptor, recordObject, keyIndex);
            } else {
                recorder.recordApi(methodDescriptor);
            }
            recorder.recordException(throwable);

            // find the target node
            if (result instanceof Future && result instanceof OperationAccessor) {
                Operation op = ((OperationAccessor)result)._$PINPOINT$_getOperation();

                if (op != null) {
                    MemcachedNode handlingNode = op.getHandlingNode();
                    SocketAddress socketAddress = handlingNode.getSocketAddress();

                    if (socketAddress instanceof InetSocketAddress) {
                        InetSocketAddress address = (InetSocketAddress) socketAddress;
                        recorder.recordEndPoint(address.getHostName() + ":" + address.getPort());
                    }
                } else {
                    logger.info("operation not found");
                }
            }

            if (target instanceof ServiceCodeAccessor) {
                // determine the service type
                String serviceCode = ((ServiceCodeAccessor)target)._$PINPOINT$_getServiceCode();
                if (serviceCode != null) {
                    recorder.recordDestinationId(serviceCode);
                    recorder.recordServiceType(ArcusConstants.ARCUS);
                } else {
                    recorder.recordDestinationId("MEMCACHED");
                    recorder.recordServiceType(ArcusConstants.MEMCACHED);
                }
            } else {
                recorder.recordDestinationId("MEMCACHED");
                recorder.recordServiceType(ArcusConstants.MEMCACHED);
            }

            try {
                if (isAsynchronousInvocation(target, args, result, throwable)) {
                    // set asynchronous trace
                    this.traceContext.getAsyncId();
                    final AsyncTraceId asyncTraceId = trace.getAsyncTraceId();
                    recorder.recordNextAsyncId(asyncTraceId.getAsyncId());
                    ((AsyncTraceIdAccessor)result)._$PINPOINT$_setAsyncTraceId(asyncTraceId);
                    if (isDebug) {
                        logger.debug("Set asyncTraceId metadata {}", asyncTraceId);
                    }
                }
            } catch (Throwable t) {
                logger.warn("Failed to BEFORE process. {}", t.getMessage(), t);
            }
        } catch (Throwable th) {
            if (logger.isWarnEnabled()) {
                logger.warn("AFTER error. Caused:{}", th.getMessage(), th);
            }
        } finally {
            trace.traceBlockEnd();
        }
    }

    private boolean isAsynchronousInvocation(final Object target, final Object[] args, Object result, Throwable throwable) {
        if (throwable != null || result == null) {
            return false;
        }

        if (!(result instanceof AsyncTraceIdAccessor)) {
            logger.debug("Invalid result object. Need accessor({}).", AsyncTraceIdAccessor.class.getName());
            return false;
        }

        return true;
    }
}
