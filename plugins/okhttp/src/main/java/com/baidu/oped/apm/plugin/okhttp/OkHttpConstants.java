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
package com.baidu.oped.apm.plugin.okhttp;

import static com.baidu.oped.apm.common.trace.ServiceTypeProperty.RECORD_STATISTICS;

import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.common.trace.ServiceTypeFactory;


/**
 * 
 * @author jaehong.kim
 *
 */
public final class OkHttpConstants {
    private OkHttpConstants() {
    }

    public static final ServiceType OK_HTTP_CLIENT = ServiceTypeFactory.of(9058, "OK_HTTP_CLIENT", RECORD_STATISTICS);
    public static final ServiceType OK_HTTP_CLIENT_INTERNAL = ServiceTypeFactory.of(9059, "OK_HTTP_CLIENT_INTERNAL", "OK_HTTP_CLIENT");

    public static final String BASIC_METHOD_INTERCEPTOR = "com.baidu.oped.apm.bootstrap.interceptor.BasicMethodInterceptor";
    public static final String METADATA_ASYNC_TRACE_ID = "com.baidu.oped.apm.bootstrap.interceptor.AsyncTraceIdAccessor";
    public static final String SEND_REQUEST_SCOPE = "SendRequestScope";
    public static final String CALL_SCOPE = "CallScope";

    public static final String FIELD_USER_REQUEST = "userRequest";
    public static final String FIELD_USER_RESPONSE = "userResponse";
    public static final String FIELD_CONNECTION = "connection";
    public static final String FIELD_HTTP_URL = "url";
}
