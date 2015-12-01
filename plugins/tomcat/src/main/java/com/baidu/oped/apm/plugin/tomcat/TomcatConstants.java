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
package com.baidu.oped.apm.plugin.tomcat;

import static com.baidu.oped.apm.common.trace.ServiceTypeProperty.*;

import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.common.trace.ServiceTypeFactory;

/**
 * @author Jongho Moon
 * @author jaehong.kim
 *
 */
public final class TomcatConstants {
    private TomcatConstants() {
    }

    public static final String TYPE_NAME = "TOMCAT";
    public static final ServiceType TOMCAT = ServiceTypeFactory.of(1010, "TOMCAT", RECORD_STATISTICS);
    public static final ServiceType TOMCAT_METHOD = ServiceTypeFactory.of(1011, "TOMCAT_METHOD");

    public static final String METADATA_TRACE = "com.baidu.oped.apm.plugin.tomcat.TraceAccessor";
    public static final String METADATA_ASYNC = "com.baidu.oped.apm.plugin.tomcat.AsyncAccessor";
    public static final String METADATA_ASYNC_TRACE_ID = "com.baidu.oped.apm.bootstrap.interceptor.AsyncTraceIdAccessor";

    public static final String ATTRIBUTE_APM_TRACE = "APM_TRACE";
    public static final String TOMCAT_SERVLET_ASYNC_SCOPE = "TomcatServletAsyncScope";
}