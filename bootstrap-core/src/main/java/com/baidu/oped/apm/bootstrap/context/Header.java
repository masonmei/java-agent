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

package com.baidu.oped.apm.bootstrap.context;

import com.baidu.oped.apm.common.util.DelegateEnumeration;
import com.baidu.oped.apm.common.util.EmptyEnumeration;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author emeroad
 */
public enum Header {

    HTTP_TRACE_ID("Apm-TraceID"),
    HTTP_SPAN_ID("Apm-SpanID"),
    HTTP_PARENT_SPAN_ID("Apm-pSpanID"),
    HTTP_SAMPLED("Apm-Sampled"),
    HTTP_FLAGS("Apm-Flags"),
    HTTP_PARENT_APPLICATION_NAME("Apm-pAppName"),
    HTTP_PARENT_APPLICATION_TYPE("Apm-pAppType"),
    HTTP_HOST("Apm-Host");

    private String name;

    Header(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    private static final Map<String, Header> NAME_SET = createMap();

    private static Map<String, Header> createMap() {
        Header[] headerList = values();
        Map<String, Header> map = new HashMap<String, Header>();
        for (Header header : headerList) {
            map.put(header.name, header);
        }
        return map;
    }

    public static Header getHeader(String name) {
        if (name == null) {
            return null;
        }
        if (!startWithApmHeader(name)) {
            return null;
        }
        return NAME_SET.get(name);
    }



    public static boolean hasHeader(String name) {
        return getHeader(name) != null;
    }

    public static Enumeration getHeaders(String name) {
        if (name == null) {
            return null;
        }
        final Header header = getHeader(name);
        if (header == null) {
            return null;
        }
        // if apm header
        return new EmptyEnumeration();
    }

    public static Enumeration filteredHeaderNames(final Enumeration enumeration) {
        return new DelegateEnumeration(enumeration, FILTER);
    }

    private static DelegateEnumeration.Filter FILTER = new DelegateEnumeration.Filter() {
        @Override
        public boolean filter(Object o) {
            if (o instanceof String) {
                return hasHeader((String )o);
            }
            return false;
        }
    };

    private static boolean startWithApmHeader(String name) {
        return name.startsWith("Apm-");
    }
}
