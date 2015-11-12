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

package com.baidu.oped.apm.bootstrap.plugin.jdbc.interceptor;

import com.baidu.oped.apm.bootstrap.context.DatabaseInfo;
import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.SpanEventRecorder;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.interceptor.SpanEventSimpleAroundInterceptorForPlugin;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.TargetMethod;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.TargetMethods;
import com.baidu.oped.apm.bootstrap.plugin.jdbc.DatabaseInfoAccessor;
import com.baidu.oped.apm.bootstrap.plugin.jdbc.UnKnownDatabaseInfo;

/**
 * protected int executeUpdate(String sql, boolean isBatch, boolean returnGeneratedKeys)
 *
 * @author netspider
 * @author emeroad
 */
@TargetMethods({
        @TargetMethod(name="executeUpdate", paramTypes={ "java.lang.String" }),
        @TargetMethod(name="executeUpdate", paramTypes={ "java.lang.String", "int" }),
        @TargetMethod(name="execute", paramTypes={ "java.lang.String" }),
        @TargetMethod(name="execute", paramTypes={ "java.lang.String", "int" })
})
public class StatementExecuteUpdateInterceptor extends SpanEventSimpleAroundInterceptorForPlugin {
    
    public StatementExecuteUpdateInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        super(traceContext, descriptor);
    }

    @Override
    public void doInBeforeTrace(SpanEventRecorder recorder, Object target, Object[] args) {
        DatabaseInfo databaseInfo = (target instanceof DatabaseInfoAccessor) ? ((DatabaseInfoAccessor)target)._$PINPOINT$_getDatabaseInfo() : null;
        
        if (databaseInfo == null) {
            databaseInfo = UnKnownDatabaseInfo.INSTANCE;
        }

        recorder.recordServiceType(databaseInfo.getExecuteQueryType());
        recorder.recordEndPoint(databaseInfo.getMultipleHost());
        recorder.recordDestinationId(databaseInfo.getDatabaseId());

        recorder.recordApi(methodDescriptor);
        if (args != null && args.length > 0) {
            Object arg = args[0];
            if (arg instanceof String) {
                recorder.recordSqlInfo((String) arg);
            }
        }
    }


    @Override
    public void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        recorder.recordException(throwable);
    }
}