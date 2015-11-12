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

import org.junit.Assert;
import org.junit.Test;

import com.baidu.oped.apm.bootstrap.instrument.DefaultInterceptorGroupDefinition;
import com.baidu.oped.apm.bootstrap.interceptor.group.AttachmentFactory;
import com.baidu.oped.apm.bootstrap.interceptor.group.ExecutionPolicy;
import com.baidu.oped.apm.bootstrap.interceptor.group.InterceptorGroupInvocation;

public class ThreadLocalScopePoolTest {

    @Test
    public void testGetScope() throws Exception {

        ScopePool pool = new ThreadLocalScopePool();
        InterceptorGroupInvocation scope = pool.getScope(new DefaultInterceptorGroupDefinition("test"));
        Assert.assertTrue(scope instanceof ThreadLocalScope);

        Assert.assertEquals("name", scope.getName(), "test");
    }

    @Test
     public void testAttachment() throws Exception {

        ScopePool pool = new ThreadLocalScopePool();
        InterceptorGroupInvocation scope = pool.getScope(new DefaultInterceptorGroupDefinition("test"));

        scope.tryEnter(ExecutionPolicy.BOUNDARY);
        scope.tryEnter(ExecutionPolicy.BOUNDARY);
        
        Assert.assertNull(scope.getAttachment());
        scope.setAttachment("test");
        
        scope.canLeave(ExecutionPolicy.BOUNDARY);
        Assert.assertEquals(scope.getAttachment(), "test");
        
        Assert.assertTrue(scope.canLeave(ExecutionPolicy.BOUNDARY));
        scope.leave(ExecutionPolicy.BOUNDARY);
        
        Assert.assertEquals("name", scope.getName(), "test");
    }


    @Test
    public void testGetOrCreate() throws Exception {
        ScopePool pool = new ThreadLocalScopePool();
        InterceptorGroupInvocation scope= pool.getScope(new DefaultInterceptorGroupDefinition("test"));
        
        scope.tryEnter(ExecutionPolicy.BOUNDARY);
        scope.tryEnter(ExecutionPolicy.BOUNDARY);

        Assert.assertNull(scope.getAttachment());
        Assert.assertEquals(scope.getOrCreateAttachment(new AttachmentFactory() {
            @Override
            public Object createAttachment() {
                return "test";
            };
        }), "test");
        
        scope.canLeave(ExecutionPolicy.BOUNDARY);
        Assert.assertEquals(scope.getAttachment(), "test");
        Assert.assertTrue(scope.canLeave(ExecutionPolicy.BOUNDARY));
        scope.leave(ExecutionPolicy.BOUNDARY);
        
        Assert.assertEquals("name", scope.getName(), "test");
    }
}