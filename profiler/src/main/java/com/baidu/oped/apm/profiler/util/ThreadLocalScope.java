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

package com.baidu.oped.apm.profiler.util;

import com.baidu.oped.apm.bootstrap.instrument.InterceptorGroupDefinition;
import com.baidu.oped.apm.bootstrap.interceptor.group.AttachmentFactory;
import com.baidu.oped.apm.bootstrap.interceptor.group.ExecutionPolicy;
import com.baidu.oped.apm.bootstrap.interceptor.group.InterceptorGroupInvocation;
import com.baidu.oped.apm.profiler.interceptor.group.DefaultInterceptorGroupInvocation;

/**
 * @author emeroad
 */
public class ThreadLocalScope implements InterceptorGroupInvocation {

    private final NamedThreadLocal<InterceptorGroupInvocation> scope;


    public ThreadLocalScope(final InterceptorGroupDefinition scopeDefinition) {
        if (scopeDefinition == null) {
            throw new NullPointerException("scopeDefinition must not be null");
        }
        
        this.scope = new NamedThreadLocal<InterceptorGroupInvocation>(scopeDefinition.getName()) {
            @Override
            protected InterceptorGroupInvocation initialValue() {
                return new DefaultInterceptorGroupInvocation(scopeDefinition.getName());
            }
        };
    }
    
    @Override
    public void leave(ExecutionPolicy policy) {
        final InterceptorGroupInvocation localScope = getLocalScope();
        localScope.leave(policy);
    }

    @Override
    public boolean tryEnter(ExecutionPolicy policy) {
        final InterceptorGroupInvocation localScope = getLocalScope();
        return localScope.tryEnter(policy);
    }

    @Override
    public boolean canLeave(ExecutionPolicy policy) {
        final InterceptorGroupInvocation localScope = getLocalScope();
        return localScope.canLeave(policy);
    }

    protected InterceptorGroupInvocation getLocalScope() {
        return scope.get();
    }


    @Override
    public String getName() {
        return scope.getName();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ThreadLocalScope{");
        sb.append("scope=").append(scope.getName());
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean isActive() {
        final InterceptorGroupInvocation localScope = getLocalScope();
        return localScope.isActive();
    }

    @Override
    public Object setAttachment(Object attachment) {
        final InterceptorGroupInvocation localScope = getLocalScope();
        return localScope.setAttachment(attachment);
    }

    @Override
    public Object getAttachment() {
        final InterceptorGroupInvocation localScope = getLocalScope();
        return localScope.getAttachment();
    }
    
    @Override
    public Object getOrCreateAttachment(AttachmentFactory factory) {
        final InterceptorGroupInvocation localScope = getLocalScope();
        return localScope.getOrCreateAttachment(factory);
    }

    @Override
    public Object removeAttachment() {
        final InterceptorGroupInvocation localScope = getLocalScope();
        return localScope.removeAttachment();
    }
}