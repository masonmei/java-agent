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

package com.baidu.oped.apm.plugin.commons.dbcp.interceptor;

import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.SpanEventRecorder;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.SpanEventSimpleAroundInterceptorForPlugin;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.Group;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.TargetMethod;
import com.baidu.oped.apm.plugin.commons.dbcp.CommonsDbcpPlugin;

/**
 * Maybe we should trace get of Datasource.
 * @author emeroad
 */
@Group(CommonsDbcpPlugin.DBCP_GROUP)
@TargetMethod(name="close")
public class DataSourceCloseInterceptor extends SpanEventSimpleAroundInterceptorForPlugin {

    public DataSourceCloseInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        super(traceContext, descriptor);
    }

    @Override
    public void doInBeforeTrace(SpanEventRecorder recorder, final Object target, Object[] args) {
    }

    @Override
    public void doInAfterTrace(SpanEventRecorder trace, Object target, Object[] args, Object result, Throwable throwable) {
        trace.recordServiceType(CommonsDbcpPlugin.DBCP_SERVICE_TYPE);
        trace.recordApi(getMethodDescriptor());
        trace.recordException(throwable);
    }
}
