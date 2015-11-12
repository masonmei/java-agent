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

package com.baidu.oped.apm.plugin.thrift;

import static com.baidu.oped.apm.common.trace.ServiceTypeProperty.*;

import java.util.regex.Pattern;

import com.baidu.oped.apm.common.trace.AnnotationKey;
import com.baidu.oped.apm.common.trace.AnnotationKeyFactory;
import com.baidu.oped.apm.common.trace.AnnotationKeyProperty;
import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.common.trace.ServiceTypeFactory;

/**
 * @author HyunGil Jeong
 */
public final class ThriftConstants {
    private ThriftConstants() {
    }

    public static final ServiceType THRIFT_SERVER = ServiceTypeFactory.of(1100, "THRIFT_SERVER", RECORD_STATISTICS);
    public static final ServiceType THRIFT_CLIENT = ServiceTypeFactory.of(9100, "THRIFT_CLIENT", RECORD_STATISTICS);
    public static final ServiceType THRIFT_SERVER_INTERNAL = ServiceTypeFactory.of(1101, "THRIFT_SERVER_INTERNAL", "THRIFT_SERVER");
    public static final ServiceType THRIFT_CLIENT_INTERNAL = ServiceTypeFactory.of(9101, "THRIFT_CLIENT_INTERNAL", "THRIFT_CLIENT");
    
    public static final AnnotationKey THRIFT_URL = AnnotationKeyFactory.of(80, "thrift.url");
    public static final AnnotationKey THRIFT_ARGS = AnnotationKeyFactory.of(81, "thrift.args", AnnotationKeyProperty.VIEW_IN_RECORD_SET);
    public static final AnnotationKey THRIFT_RESULT = AnnotationKeyFactory.of(82, "thrift.result", AnnotationKeyProperty.VIEW_IN_RECORD_SET);
    
    public static final String UNKNOWN_METHOD_NAME = "unknown";
    public static final String UNKNOWN_METHOD_URI = "/" + UNKNOWN_METHOD_NAME;
    public static final String UNKNOWN_ADDRESS = "Unknown";
    
    public static final Pattern PROCESSOR_PATTERN = Pattern.compile("\\$Processor");
    public static final Pattern ASYNC_PROCESSOR_PATTERN = Pattern.compile("\\$AsyncProcessor");
    public static final Pattern CLIENT_PATTERN = Pattern.compile("\\$Client");
    public static final Pattern ASYNC_METHOD_CALL_PATTERN = Pattern.compile("\\$AsyncClient\\$");
    
    public static final String ATTRIBUTE_CONFIG = "thriftPluginConfig";
    
    // field names
    public static final String T_ASYNC_METHOD_CALL_FIELD_TRANSPORT = "transport";
    public static final String FRAME_BUFFER_FIELD_TRANS_ = "trans_";
    public static final String FRAME_BUFFER_FIELD_IN_TRANS_ = "inTrans_";
    
}
