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

package com.baidu.oped.apm.plugin.mybatis.interceptor;

import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.SpanEventRecorder;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.SpanEventSimpleAroundInterceptorForPlugin;
import com.baidu.oped.apm.plugin.mybatis.MyBatisPlugin;

/**
 * @author HyunGil Jeong
 */
public class SqlSessionOperationInterceptor extends SpanEventSimpleAroundInterceptorForPlugin {

    public SqlSessionOperationInterceptor(TraceContext context, MethodDescriptor descriptor) {
        super(context, descriptor);
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, Object target, Object[] args) {
        // do nothing
    }

    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result,
            Throwable throwable) {
        recorder.recordServiceType(MyBatisPlugin.MYBATIS);
        recorder.recordException(throwable);
        if (args != null && args.length > 0) {
            recorder.recordApiCachedString(getMethodDescriptor(), (String)args[0], 0);
        } else {
            recorder.recordApi(getMethodDescriptor());
        }
    }

}