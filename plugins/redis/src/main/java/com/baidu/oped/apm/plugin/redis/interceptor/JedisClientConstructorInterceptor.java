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
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.AroundInterceptor;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.plugin.redis.EndPointAccessor;
import com.baidu.oped.apm.plugin.redis.RedisConstants;

/**
 * Jedis client(redis client) constructor interceptor - trace endPoint
 * 
 * @author jaehong.kim
 *
 */
public class JedisClientConstructorInterceptor implements AroundInterceptor {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    public JedisClientConstructorInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor) {
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        try {
            if (!validate(target, args)) {
                return;
            }

            final StringBuilder endPoint = new StringBuilder();
            // first argument is host
            endPoint.append(args[0]);

            // second argument is port
            if (args.length >= 2 && args[1] instanceof Integer) {
                endPoint.append(":").append(args[1]);
            } else {
                // set default port
                endPoint.append(":").append(6379);
            }
            ((EndPointAccessor)target)._$PINPOINT$_setEndPoint(endPoint.toString());
        } catch (Throwable t) {
            logger.warn("Failed to BEFORE process. {}", t.getMessage(), t);
        }
    }

    private boolean validate(final Object target, final Object[] args) {
        if (args == null || args.length == 0 || args[0] == null) {
            logger.debug("Invalid arguments. Null or not found args({}).", args);
            return false;
        }

        if (!(args[0] instanceof String)) {
            logger.debug("Invalid arguments. Expect String but args[0]({}).", args[0]);
            return false;
        }

        if (!(target instanceof EndPointAccessor)) {
            logger.debug("Invalid target object. Need field accessor({}).", RedisConstants.METADATA_END_POINT);
            return false;
        }

        return true;
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
    }
}