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
package com.baidu.oped.apm.plugin;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.baidu.oped.apm.bootstrap.plugin.test.PluginTestVerifier;
import com.baidu.oped.apm.bootstrap.plugin.test.PluginTestVerifierHolder;
import com.baidu.oped.apm.test.plugin.Dependency;
import com.baidu.oped.apm.test.plugin.ApmConfig;
import com.baidu.oped.apm.test.plugin.ApmPluginTestSuite;

@RunWith(ApmPluginTestSuite.class)
@Dependency({"com.fasterxml.jackson.core:jackson-databind:[2.6.1]"})
@ApmConfig("apm-disabled-plugin-test.config")
public class PluginDisableIT {

    @Test
    public void test() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", "jackson");
        
        mapper.writeValueAsString(map);
        mapper.writeValueAsBytes(map);
        
        ObjectWriter writer = mapper.writer();
        
        writer.writeValueAsString(map);
        writer.writeValueAsBytes(map);

        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.printCache();
        
        verifier.verifyTraceCount(0);
    }
}


