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

package com.baidu.oped.apm.profiler;

import com.baidu.oped.apm.bootstrap.util.NetworkUtils;
import com.baidu.oped.apm.common.ApmConstants;
import com.baidu.oped.apm.common.Version;
import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.common.util.BytesUtils;
import com.baidu.oped.apm.common.util.JvmUtils;
import com.baidu.oped.apm.common.util.SimpleProperty;
import com.baidu.oped.apm.common.util.SystemProperty;
import com.baidu.oped.apm.common.util.SystemPropertyKey;
import com.baidu.oped.apm.profiler.util.RuntimeMXBeanUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author emeroad
 */
public class AgentInformationFactory {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SimpleProperty systemProperty = SystemProperty.INSTANCE;

    public AgentInformationFactory() {
    }

    public AgentInformation createAgentInformation(ServiceType serverType) {
        if (serverType == null) {
            throw new NullPointerException("serverType must not be null");
        }
        // For compatibility issue, use machineName as agentId when agengId is not provided.
        // This could be a problem if more than one server instances run on a box.
        final String machineName = NetworkUtils.getHostName();
        final String hostIp = NetworkUtils.getHostIp();
        final String agentId = getId("apm.agentId", machineName, ApmConstants.AGENT_NAME_MAX_LEN);
        final String applicationName = getId("apm.applicationName", "UnknownApplicationName", ApmConstants.APPLICATION_NAME_MAX_LEN);
        final long startTime = RuntimeMXBeanUtils.getVmStartTime();
        final int pid = RuntimeMXBeanUtils.getPid();
        final String jvmVersion = JvmUtils.getSystemProperty(SystemPropertyKey.JAVA_VERSION);
        return new AgentInformation(agentId, applicationName, startTime, pid, machineName, hostIp, serverType, jvmVersion, Version.VERSION);
    }

    private String getId(String key, String defaultValue, int maxlen) {
        String value = systemProperty.getProperty(key, defaultValue);
        validateId(value, key, maxlen);
        return value;
    }

    private void validateId(String id, String idName, int maxlen) {
        if (id == null) {
            throw new NullPointerException("id must not be null");
        }

        // TODO AgengId should be validated BEFORE bootclass.
        // or agent should stop when validation is failed here.
        final byte[] bytes = BytesUtils.toBytes(id);
        if (bytes.length > maxlen) {
            logger.warn("{} is too long(1~24). value={}", idName, id);
        }
    }
}
