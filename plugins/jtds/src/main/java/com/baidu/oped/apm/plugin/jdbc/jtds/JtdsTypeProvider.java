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

import com.baidu.oped.apm.common.trace.AnnotationKey;
import com.baidu.oped.apm.common.trace.AnnotationKeyMatchers;
import com.baidu.oped.apm.common.trace.TraceMetadataProvider;
import com.baidu.oped.apm.common.trace.TraceMetadataSetupContext;

/**
 * @author Jongho Moon
 *
 */
public class JtdsTypeProvider implements TraceMetadataProvider {

    @Override
    public void setup(TraceMetadataSetupContext context) {
        context.addServiceType(JtdsConstants.MSSQL, AnnotationKeyMatchers.exact(AnnotationKey.ARGS0));
        context.addServiceType(JtdsConstants.MSSQL_EXECUTE_QUERY, AnnotationKeyMatchers.exact(AnnotationKey.ARGS0));
    }

}
