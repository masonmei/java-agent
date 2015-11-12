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

package com.baidu.oped.apm.plugin.thrift.interceptor.server;

import static com.baidu.oped.apm.plugin.thrift.ThriftScope.THRIFT_SERVER_SCOPE;

import org.apache.thrift.ProcessFunction;
import org.apache.thrift.protocol.TProtocol;

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
import com.baidu.oped.apm.plugin.thrift.field.accessor.ServerMarkerFlagFieldAccessor;

/**
 * This interceptor marks the starting point for tracing {@link org.apache.thrift.ProcessFunction ProcessFunction} and creates the client call context to share
 * with other interceptors within the current scope.
 * <p>
 * <tt>TBaseProcessorProcessInterceptor</tt> -> <b><tt>ProcessFunctionProcessInterceptor</tt></b> -> <tt>TProtocolReadFieldBeginInterceptor</tt> <->
 * <tt>TProtocolReadTTypeInterceptor</tt> -> <tt>TProtocolReadMessageEndInterceptor</tt>
 * <p>
 * Based on Thrift 0.9.x
 * 
 * @author HyunGil Jeong
 * 
 * @see com.baidu.oped.apm.plugin.thrift.interceptor.server.TBaseProcessorProcessInterceptor TBaseProcessorProcessInterceptor
 * @see com.baidu.oped.apm.plugin.thrift.interceptor.tprotocol.server.TProtocolReadFieldBeginInterceptor TProtocolReadFieldBeginInterceptor
 * @see com.baidu.oped.apm.plugin.thrift.interceptor.tprotocol.server.TProtocolReadTTypeInterceptor TProtocolReadTTypeInterceptor
 * @see com.baidu.oped.apm.plugin.thrift.interceptor.tprotocol.server.TProtocolReadMessageEndInterceptor TProtocolReadMessageEndInterceptor
 */
@Group(value = THRIFT_SERVER_SCOPE, executionPolicy = ExecutionPolicy.INTERNAL)
public class ProcessFunctionProcessInterceptor implements AroundInterceptor {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final InterceptorGroup group;

    public ProcessFunctionProcessInterceptor(@Name(THRIFT_SERVER_SCOPE) InterceptorGroup group) {
        this.group = group;
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }
        // process(int seqid, TProtocol iprot, TProtocol oprot, I iface)
        if (args.length != 4) {
            return;
        }
        String methodName = ThriftConstants.UNKNOWN_METHOD_NAME;
        if (target instanceof ProcessFunction) {
            ProcessFunction<?, ?> processFunction = (ProcessFunction<?, ?>)target;
            methodName = processFunction.getMethodName();
        }
        ThriftClientCallContext clientCallContext = new ThriftClientCallContext(methodName);
        InterceptorGroupInvocation currentTransaction = this.group.getCurrentInvocation();
        currentTransaction.setAttachment(clientCallContext);
        // Set server marker - server handlers may create a client to call another Thrift server.
        // When this happens, TProtocol interceptors for clients are triggered since technically they're still within THRIFT_SERVER_SCOPE.
        // We set the marker inside server's input protocol to safeguard against such cases.
        Object iprot = args[1];
        if (validateInputProtocol(iprot)) {
            ((ServerMarkerFlagFieldAccessor)iprot)._$PINPOINT$_setServerMarkerFlag(true);
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }
        // Unset server marker
        Object iprot = args[1];
        if (validateInputProtocol(iprot)) {
            ((ServerMarkerFlagFieldAccessor)iprot)._$PINPOINT$_setServerMarkerFlag(false);
        }
    }

    private boolean validateInputProtocol(Object iprot) {
        if (iprot instanceof TProtocol) {
            if (iprot instanceof ServerMarkerFlagFieldAccessor) {
                return true;
            } else {
                if (isDebug) {
                    logger.debug("Invalid target object. Need field accessor({}).", ServerMarkerFlagFieldAccessor.class.getName());
                }
                return false;
            }
        }
        return false;
    }

}
