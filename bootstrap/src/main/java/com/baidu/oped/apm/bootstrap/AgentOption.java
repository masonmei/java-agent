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

package com.baidu.oped.apm.bootstrap;

import java.lang.instrument.Instrumentation;
import java.net.URL;

import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;
import com.baidu.oped.apm.common.service.AnnotationKeyRegistryService;
import com.baidu.oped.apm.common.service.ServiceTypeRegistryService;

/**
 * @author emeroad
 */
public interface AgentOption {

    String getAgentArgs();

    Instrumentation getInstrumentation();

    ProfilerConfig getProfilerConfig();

    URL[] getPluginJars();

    String getBootStrapJarPath();

    ServiceTypeRegistryService getServiceTypeRegistryService();
    
    AnnotationKeyRegistryService getAnnotationKeyRegistryService();
}
