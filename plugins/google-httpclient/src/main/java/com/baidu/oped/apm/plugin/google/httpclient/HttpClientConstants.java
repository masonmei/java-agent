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
package com.baidu.oped.apm.plugin.google.httpclient;

import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.common.trace.ServiceTypeFactory;


/**
 * 
 * @author jaehong.kim
 *
 */
public final class HttpClientConstants {
    private HttpClientConstants() {
    }

    public static final ServiceType HTTP_CLIENT_INTERNAL = ServiceTypeFactory.of(9054, "GOOGLE_HTTP_CLIENT_INTERNAL", "GOOGLE_HTTP_CLIENT");
    
    public static final String METADATA_ASYNC_TRACE_ID = "com.baidu.oped.apm.bootstrap.interceptor.AsyncTraceIdAccessor";
    public static final String EXECUTE_ASYNC_SCOPE = "ExecuteAsyncScope"; 
}
