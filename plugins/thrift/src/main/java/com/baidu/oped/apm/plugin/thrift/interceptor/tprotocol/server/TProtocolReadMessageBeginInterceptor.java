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

package com.baidu.oped.apm.plugin.thrift.interceptor.tprotocol.server;

import static com.baidu.oped.apm.plugin.thrift.ThriftScope.THRIFT_SERVER_SCOPE;

import org.apache.thrift.protocol.TMessage;

import com.baidu.oped.apm.bootstrap.interceptor.AroundInterceptor;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.Group;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.Name;
import com.baidu.oped.apm.bootstrap.interceptor.group.ExecutionPolicy;
import com.baidu.oped.apm.bootstrap.interceptor.group.InterceptorGroup;
import com.baidu.oped.apm.bootstrap.interceptor.group.InterceptorGroupInvocation;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.plugin.thrift.ThriftClientCallContext;
import com.baidu.oped.apm.plugin.thrift.ThriftConstants;
import com.baidu.oped.apm.plugin.thrift.field.accessor.AsyncMarkerFlagFieldAccessor;

/**
 * This interceptor retrieves the method name called by the client and stores it for other interceptors in the chain to use.
 * <p>
 * <tt>TBaseAsyncProcessorProcessInterceptor</tt> -> <b><tt>TProtocolReadMessageBeginInterceptor</tt></b> -> <tt>TProtocolReadFieldBeginInterceptor</tt> <->
 * <tt>TProtocolReadTTypeInterceptor</tt> -> <tt>TProtocolReadMessageEndInterceptor</tt>
 * <p>
 * Based on Thrift 0.8.0+
 * 
 * @author HyunGil Jeong
 * 
 * @see com.baidu.oped.apm.plugin.thrift.interceptor.tprotocol.server.TProtocolReadMessageBeginInterceptor TProtocolReadMessageBeginInterceptor
 * @see com.baidu.oped.apm.plugin.thrift.interceptor.tprotocol.server.TProtocolReadFieldBeginInterceptor TProtocolReadFieldBeginInterceptor
 * @see com.baidu.oped.apm.plugin.thrift.interceptor.tprotocol.server.TProtocolReadTTypeInterceptor TProtocolReadTTypeInterceptor
 * @see com.baidu.oped.apm.plugin.thrift.interceptor.tprotocol.server.TProtocolReadMessageEndInterceptor TProtocolReadMessageEndInterceptor
 */
@Group(value = THRIFT_SERVER_SCOPE, executionPolicy = ExecutionPolicy.INTERNAL)
public class TProtocolReadMessageBeginInterceptor implements AroundInterceptor {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final InterceptorGroup group;

    public TProtocolReadMessageBeginInterceptor(@Name(THRIFT_SERVER_SCOPE) InterceptorGroup group) {
        this.group = group;
    }

    @Override
    public void before(Object target, Object[] args) {
        // Do nothing
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }
        if (!validate(target)) {
            return;
        }
        final boolean shouldTrace = ((AsyncMarkerFlagFieldAccessor)target)._$PINPOINT$_getAsyncMarkerFlag();
        if (shouldTrace) {
            String methodName = ThriftConstants.UNKNOWN_METHOD_NAME;
            if (result instanceof TMessage) {
                TMessage message = (TMessage)result;
                methodName = message.name;
            }
            ThriftClientCallContext clientCallContext = new ThriftClientCallContext(methodName);
            InterceptorGroupInvocation currentTransaction = this.group.getCurrentInvocation();
            currentTransaction.setAttachment(clientCallContext);
        }
    }

    private boolean validate(Object target) {
        if (!(target instanceof AsyncMarkerFlagFieldAccessor)) {
            if (isDebug) {
                logger.debug("Invalid target object. Need field accessor({}).", AsyncMarkerFlagFieldAccessor.class.getName());
            }
            return false;
        }
        return true;
    }

}
