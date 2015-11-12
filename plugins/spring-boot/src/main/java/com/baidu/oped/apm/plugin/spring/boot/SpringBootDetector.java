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

package com.baidu.oped.apm.plugin.spring.boot;

import com.baidu.oped.apm.bootstrap.plugin.ApplicationTypeDetector;
import com.baidu.oped.apm.bootstrap.resolver.ConditionProvider;
import com.baidu.oped.apm.common.trace.ServiceType;

/**
 * @author HyunGil Jeong
 */
public class SpringBootDetector implements ApplicationTypeDetector {

    @Override
    public ServiceType getApplicationType() {
        return SpringBootConstants.SERVICE_TYPE;
    }

    @Override
    public boolean detect(ConditionProvider provider) {
        String bootstrapMainClass = provider.getMainClass();
        return bootstrapMainClass.startsWith(SpringBootConstants.BOOTSTRAP_MAIN_PREFIX);
    }

}
