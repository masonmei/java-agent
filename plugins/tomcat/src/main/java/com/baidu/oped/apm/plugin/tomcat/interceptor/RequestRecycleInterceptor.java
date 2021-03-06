/**
 * Copyright 2014 NAVER Corp.
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
package com.baidu.oped.apm.plugin.tomcat.interceptor;

import com.baidu.oped.apm.bootstrap.context.Trace;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentMethod;
import com.baidu.oped.apm.bootstrap.interceptor.AroundInterceptor;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.plugin.tomcat.AsyncAccessor;
import com.baidu.oped.apm.plugin.tomcat.TraceAccessor;

/**
 * 
 * @author jaehong.kim
 *
 */
public class RequestRecycleInterceptor implements AroundInterceptor {

    private PLogger logger = PLoggerFactory.getLogger(this.getClass());

    private InstrumentMethod targetMethod;

    public RequestRecycleInterceptor(InstrumentMethod targetMethod) {
        this.targetMethod = targetMethod;
    }

    @Override
    public void before(Object target, Object[] args) {
        logger.beforeInterceptor(target, target.getClass().getName(), targetMethod.getName(), "", args);
        try {
            if (target instanceof AsyncAccessor) {
                // reset
                ((AsyncAccessor) target)._$APM$_setAsync(Boolean.FALSE);
            }

            if (target instanceof TraceAccessor) {
                final Trace trace = ((TraceAccessor) target)._$APM$_getTrace();
                if (trace != null && trace.canSampled()) {
                    // end of root span
                    trace.close();
                }
                // reset
                ((TraceAccessor) target)._$APM$_setTrace(null);
            }
        } catch (Throwable t) {
            logger.warn("Failed to BEFORE process. {}", t.getMessage(), t);
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
    }
}