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

package com.baidu.oped.apm.bootstrap.interceptor.group;

import com.baidu.oped.apm.bootstrap.interceptor.ApiIdAwareAroundInterceptor;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;

/**
 * @author emeroad
 */
public class GroupedApiIdAwareAroundInterceptor implements ApiIdAwareAroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(getClass());
    private final boolean debugEnabled = logger.isDebugEnabled();

    private final ApiIdAwareAroundInterceptor delegate;
    private final InterceptorGroup group;
    private final ExecutionPolicy policy;

    public GroupedApiIdAwareAroundInterceptor(ApiIdAwareAroundInterceptor delegate, InterceptorGroup group, ExecutionPolicy policy) {
        this.delegate = delegate;
        this.group = group;
        this.policy = policy;
    }

    @Override
    public void before(Object target, int apiId, Object[] args) {
        InterceptorGroupInvocation transaction = group.getCurrentInvocation();
        
        if (transaction.tryEnter(policy)) {
            this.delegate.before(target, apiId, args);
        } else {
            if (debugEnabled) {
                logger.debug("tryBefore() returns false: interceptorGroupTransaction: {}, executionPoint: {}. Skip interceptor {}", new Object[] {transaction, policy, delegate.getClass()} );
            }
        }
    }

    @Override
    public void after(Object target, int apiId, Object[] args, Object result, Throwable throwable) {
        InterceptorGroupInvocation transaction = group.getCurrentInvocation();
        
        if (transaction.canLeave(policy)) {
            this.delegate.after(target, apiId, args, result, throwable);
            transaction.leave(policy);
        } else {
            if (debugEnabled) {
                logger.debug("tryAfter() returns false: interceptorGroupTransaction: {}, executionPoint: {}. Skip interceptor {}", new Object[] {transaction, policy, delegate.getClass()} );
            }
        }
    }
}
