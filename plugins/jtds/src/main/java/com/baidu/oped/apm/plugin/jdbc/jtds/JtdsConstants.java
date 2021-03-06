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
package com.baidu.oped.apm.plugin.jdbc.jtds;

import static com.baidu.oped.apm.common.trace.ServiceTypeProperty.*;

import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.common.trace.ServiceTypeFactory;

/**
 * @author Jongho Moon
 *
 */
public final class JtdsConstants {
    private JtdsConstants() {
    }

    public static final String GROUP_JTDS = "JTDS";
    
    public static final ServiceType MSSQL = ServiceTypeFactory.of(2200, "MSSQL", "MSSQLSERVER", TERMINAL, INCLUDE_DESTINATION_ID);
    public static final ServiceType MSSQL_EXECUTE_QUERY = ServiceTypeFactory.of(2201, "MSSQL_EXECUTE_QUERY", "MSSQLSERVER", TERMINAL, RECORD_STATISTICS, INCLUDE_DESTINATION_ID);
}
