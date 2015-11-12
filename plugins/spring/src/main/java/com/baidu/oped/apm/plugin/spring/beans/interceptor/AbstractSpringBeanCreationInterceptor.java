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

package com.baidu.oped.apm.plugin.spring.beans.interceptor;

import com.baidu.oped.apm.bootstrap.instrument.Instrumentor;
import com.baidu.oped.apm.bootstrap.instrument.transformer.TransformCallback;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;

/**
 * 
 * @author Jongho Moon <jongho.moon@navercorp.com>
 */
public abstract class AbstractSpringBeanCreationInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(getClass());

    private final Instrumentor instrumentContext;
    private final TransformCallback transformer;
    private final TargetBeanFilter filter;
    
    protected AbstractSpringBeanCreationInterceptor(Instrumentor instrumentContext, TransformCallback transformer, TargetBeanFilter filter) {
        this.instrumentContext = instrumentContext;
        this.transformer = transformer;
        this.filter = filter;
    }

    protected final void processBean(String beanName, Object bean) {
        if (bean == null) {
            return;
        }
        
        Class<?> clazz = bean.getClass();
        
        if (!filter.isTarget(beanName, clazz)) {
            return;
        }
        
        // If you want to trace inherited methods, you have to retranform super classes, too.
        instrumentContext.retransform(clazz, transformer);
        
        filter.addTransformed(clazz);

        if (logger.isInfoEnabled()) {
            logger.info("Retransform {}", clazz.getName());
        }
    }
}
