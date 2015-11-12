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
package com.baidu.oped.apm.plugin.jdbc.mysql;

import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;

/**
 * @author Jongho Moon
 *
 */
public class MySqlConfig {
    private final boolean profileSetAutoCommit;
    private final boolean profileCommit;
    private final boolean profileRollback;
    private final int maxSqlBindValueSize; 

    public MySqlConfig(ProfilerConfig config) {
        this.profileSetAutoCommit = config.readBoolean("profiler.jdbc.mysql.setautocommit", false);
        this.profileCommit = config.readBoolean("profiler.jdbc.mysql.commit", false);
        this.profileRollback = config.readBoolean("profiler.jdbc.mysql.rollback", false);
        this.maxSqlBindValueSize = config.readInt("profiler.jdbc.maxsqlbindvaluesize", 1024);
    }

    public boolean isProfileSetAutoCommit() {
        return profileSetAutoCommit;
    }

    public boolean isProfileCommit() {
        return profileCommit;
    }

    public boolean isProfileRollback() {
        return profileRollback;
    }
    
    public int getMaxSqlBindValueSize() {
        return maxSqlBindValueSize;
    }
    
    @Override
    public String toString() {
        return "MySqlConfig [profileSetAutoCommit=" + profileSetAutoCommit + ", profileCommit=" + profileCommit + ", profileRollback=" + profileRollback + ", maxSqlBindValueSize=" + maxSqlBindValueSize + "]";
    };
}
