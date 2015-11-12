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

/**
 * @author emeroad
 */
import org.junit.Assert;
import org.junit.Test;

import com.baidu.oped.apm.bootstrap.instrument.DefaultInterceptorGroupDefinition;
import com.baidu.oped.apm.bootstrap.interceptor.group.ExecutionPolicy;
import com.baidu.oped.apm.bootstrap.interceptor.group.InterceptorGroupInvocation;

/**
 * @author emeroad
 */
public class ThreadLocalScopeTest {
    @Test
    public void pushPop() {
        InterceptorGroupInvocation scope = new ThreadLocalScope(new DefaultInterceptorGroupDefinition("test"));
        Assert.assertTrue(scope.tryEnter(ExecutionPolicy.BOUNDARY));
        Assert.assertFalse(scope.tryEnter(ExecutionPolicy.BOUNDARY));
        Assert.assertFalse(scope.tryEnter(ExecutionPolicy.BOUNDARY));
        
        Assert.assertTrue(scope.isActive());

        Assert.assertFalse(scope.canLeave(ExecutionPolicy.BOUNDARY));
        Assert.assertFalse(scope.canLeave(ExecutionPolicy.BOUNDARY));
        Assert.assertTrue(scope.canLeave(ExecutionPolicy.BOUNDARY));
        scope.leave(ExecutionPolicy.BOUNDARY);
    }

    @Test(expected=IllegalStateException.class)
    public void pushPopError() {
        InterceptorGroupInvocation scope = new ThreadLocalScope(new DefaultInterceptorGroupDefinition("test"));
        scope.leave(ExecutionPolicy.BOUNDARY);
    }

    @Test
    public void getName() {
        InterceptorGroupInvocation scope = new ThreadLocalScope(new DefaultInterceptorGroupDefinition("test"));
        Assert.assertEquals(scope.getName(), "test");

    }
}

