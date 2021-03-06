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

import com.baidu.oped.apm.bootstrap.config.ExcludeUrlFilter;
import com.baidu.oped.apm.bootstrap.config.Filter;
import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;

/**
 * @author Jongho Moon
 *
 */
public class TomcatConfiguration {
    private final boolean tomcatHideApmHeader;
    private Filter<String> tomcatExcludeUrlFilter;

    public TomcatConfiguration(ProfilerConfig config) {
        this.tomcatHideApmHeader = config.readBoolean("profiler.tomcat.hideapmheader", true);
        final String tomcatExcludeURL = config.readString("profiler.tomcat.excludeurl", "");
        
        if (!tomcatExcludeURL.isEmpty()) {
            this.tomcatExcludeUrlFilter = new ExcludeUrlFilter(tomcatExcludeURL);
        }
    }

    public Filter<String> getTomcatExcludeUrlFilter() {
        return tomcatExcludeUrlFilter;
    }

    public boolean isTomcatHideApmHeader() {
        return tomcatHideApmHeader;
    }
}