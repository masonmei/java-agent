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

import com.baidu.oped.apm.bootstrap.interceptor.AfterInterceptor3;
import com.baidu.oped.apm.bootstrap.interceptor.AroundInterceptor3;
import com.baidu.oped.apm.bootstrap.interceptor.BeforeInterceptor3;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;

/**
 * @author emeroad
 */
public class GroupedInterceptor3 implements AroundInterceptor3 {
    private final PLogger logger = PLoggerFactory.getLogger(getClass());
    private final boolean debugEnabled = logger.isDebugEnabled();

    private final BeforeInterceptor3 before;
    private final AfterInterceptor3 after;
    private final InterceptorGroup group;
    private final ExecutionPolicy policy;
    
    public GroupedInterceptor3(BeforeInterceptor3 before, AfterInterceptor3 after, InterceptorGroup group, ExecutionPolicy policy) {
        this.before = before;
        this.after = after;
        this.group = group;
        this.policy = policy;
    }
    
    @Override
    public void before(Object target, Object arg0, Object arg1, Object arg2) {
        InterceptorGroupInvocation transaction = group.getCurrentInvocation();
        
        if (transaction.tryEnter(policy)) {
            if (before != null) {
                before.before(target, arg0, arg1, arg2);
            }
        } else {
            if (debugEnabled) {
                logger.debug("tryBefore() returns false: interceptorGroupTransaction: {}, executionPoint: {}. Skip interceptor {}", new Object[] {transaction, policy, before == null ? null : before.getClass()} );
            }
        }
    }

    @Override
    public void after(Object target, Object arg0, Object arg1, Object arg2, Object result, Throwable throwable) {
        InterceptorGroupInvocation transaction = group.getCurrentInvocation();
        
        if (transaction.canLeave(policy)) {
            if (after != null) {
                after.after(target, arg0, arg1, arg2, result, throwable);
            }
            transaction.leave(policy);
        } else {
            if (debugEnabled) {
                logger.debug("tryAfter() returns false: interceptorGroupTransaction: {}, executionPoint: {}. Skip interceptor {}", new Object[] {transaction, policy, after == null ? null : after.getClass()} );
            }
        }
    }
}
