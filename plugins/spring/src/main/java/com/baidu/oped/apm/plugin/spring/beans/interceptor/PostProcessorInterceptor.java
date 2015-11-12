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
import com.baidu.oped.apm.bootstrap.interceptor.AfterInterceptor2;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;

/**
 * 
 * @author Jongho Moon <jongho.moon@navercorp.com>
 *
 */
public class PostProcessorInterceptor extends AbstractSpringBeanCreationInterceptor implements AfterInterceptor2 {
    private final PLogger logger = PLoggerFactory.getLogger(getClass());
    
    public PostProcessorInterceptor(Instrumentor instrumentContext, TransformCallback transformer, TargetBeanFilter filter) {
        super(instrumentContext, transformer, filter);
    }

    @Override
    public void after(Object target, Object arg0, Object beanNameObject, Object result, Throwable throwable) {
        try {
            if (!(beanNameObject instanceof String)) {
                logger.warn("invalid type:{}", beanNameObject);
                return;
            }
            final String beanName = (String) beanNameObject;
            processBean(beanName, result);
        } catch (Throwable t) {
            logger.warn("Unexpected exception", t);
        }
    }
}
