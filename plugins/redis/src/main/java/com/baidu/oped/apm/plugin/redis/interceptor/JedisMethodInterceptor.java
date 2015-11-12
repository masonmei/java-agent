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

package com.baidu.oped.apm.plugin.redis.interceptor;

import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.SpanEventRecorder;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.SpanEventSimpleAroundInterceptorForPlugin;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.Group;
import com.baidu.oped.apm.bootstrap.interceptor.group.InterceptorGroup;
import com.baidu.oped.apm.bootstrap.interceptor.group.InterceptorGroupInvocation;
import com.baidu.oped.apm.common.trace.AnnotationKey;
import com.baidu.oped.apm.plugin.redis.CommandContext;
import com.baidu.oped.apm.plugin.redis.CommandContextFactory;
import com.baidu.oped.apm.plugin.redis.EndPointAccessor;
import com.baidu.oped.apm.plugin.redis.RedisConstants;

/**
 * Jedis (redis client) method interceptor
 * 
 * @author jaehong.kim
 *
 */
@Group(value = RedisConstants.REDIS_SCOPE)
public class JedisMethodInterceptor extends SpanEventSimpleAroundInterceptorForPlugin {

    private InterceptorGroup interceptorGroup;
    private boolean io;

    public JedisMethodInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor, InterceptorGroup interceptorGroup, boolean io) {
        super(traceContext, methodDescriptor);

        this.interceptorGroup = interceptorGroup;
        this.io = io;
    }

    @Override
    public void doInBeforeTrace(SpanEventRecorder recorder, Object target, Object[] args) {
        final InterceptorGroupInvocation invocation = interceptorGroup.getCurrentInvocation();
        if (invocation != null) {
            invocation.getOrCreateAttachment(CommandContextFactory.COMMAND_CONTEXT_FACTORY);
        }
    }

    @Override
    public void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        String endPoint = null;

        if (target instanceof EndPointAccessor) {
            endPoint = ((EndPointAccessor) target)._$PINPOINT$_getEndPoint();
        }
        
        final InterceptorGroupInvocation invocation = interceptorGroup.getCurrentInvocation();
        if (invocation != null && invocation.getAttachment() != null) {
            final CommandContext commandContext = (CommandContext) invocation.getAttachment();
            logger.debug("Check command context {}", commandContext);
            if (io) {
                final StringBuilder sb = new StringBuilder();
                sb.append("write=").append(commandContext.getWriteElapsedTime());
                if (commandContext.isWriteFail()) {
                    sb.append("(fail)");
                }
                sb.append(", read=").append(commandContext.getReadElapsedTime());
                if (commandContext.isReadFail()) {
                    sb.append("(fail)");
                }
                recorder.recordAttribute(AnnotationKey.ARGS0, sb.toString());
            }
            // clear
            invocation.removeAttachment();
        }

        recorder.recordApi(getMethodDescriptor());
        recorder.recordEndPoint(endPoint != null ? endPoint : "Unknown");
        recorder.recordDestinationId(RedisConstants.REDIS.getName());
        recorder.recordServiceType(RedisConstants.REDIS);
        recorder.recordException(throwable);
    }
}