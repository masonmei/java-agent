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

package com.baidu.oped.apm.bootstrap.plugin.jdbc;

import java.util.ArrayList;
import java.util.List;

import com.baidu.oped.apm.bootstrap.context.DatabaseInfo;
import com.baidu.oped.apm.common.trace.ServiceType;

/**
 * @author emeroad
 */
public class UnKnownDatabaseInfo {
    public static final DatabaseInfo INSTANCE;

    static{
        final List<String> urls = new ArrayList<String>();
        urls.add("unknown");
        INSTANCE = new DefaultDatabaseInfo(ServiceType.UNKNOWN_DB, ServiceType.UNKNOWN_DB_EXECUTE_QUERY, "unknown", "unknown", urls, "unknown");
    }
    
    public static DatabaseInfo createUnknownDataBase(String url) {
        return createUnknownDataBase(ServiceType.UNKNOWN_DB, ServiceType.UNKNOWN_DB_EXECUTE_QUERY, url);
    }

    public static DatabaseInfo createUnknownDataBase(ServiceType type, ServiceType executeQueryType, String url) {
        List<String> list = new ArrayList<String>();
        list.add("error");
        return new DefaultDatabaseInfo(type, executeQueryType, url, url, list, "error");
    }
}
