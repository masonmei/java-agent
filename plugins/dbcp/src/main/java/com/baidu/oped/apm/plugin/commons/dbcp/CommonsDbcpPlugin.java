/**
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
package com.baidu.oped.apm.plugin.commons.dbcp;

import java.security.ProtectionDomain;

import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.Instrumentor;
import com.baidu.oped.apm.bootstrap.instrument.transformer.TransformCallback;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPlugin;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPluginSetupContext;
import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.common.trace.ServiceTypeFactory;

/**
 * @author Jongho Moon
 */
public class CommonsDbcpPlugin implements ProfilerPlugin {
    public static final ServiceType DBCP_SERVICE_TYPE = ServiceTypeFactory.of(6050, "DBCP");
    public static final String DBCP_GROUP = "DBCP_GROUP";

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        addBasicDataSourceTransformer(context);
        
        boolean profileClose = context.getConfig().readBoolean("profiler.jdbc.dbcp.connectionclose", false);
        
        if (profileClose) {
            addPoolGuardConnectionWrapperTransformer(context);
        }
    }

    private void addPoolGuardConnectionWrapperTransformer(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("org.apache.commons.dbcp.PoolingDataSource$PoolGuardConnectionWrapper", new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor pluginContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = pluginContext.getInstrumentClass(loader, className, classfileBuffer);
                target.addInterceptor("com.baidu.oped.apm.plugin.commons.dbcp.interceptor.DataSourceCloseInterceptor");
                return target.toBytecode();
            }
        });
    }

    private void addBasicDataSourceTransformer(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("org.apache.commons.dbcp.BasicDataSource", new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor pluginContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = pluginContext.getInstrumentClass(loader, className, classfileBuffer);
                target.addInterceptor("com.baidu.oped.apm.plugin.commons.dbcp.interceptor.DataSourceGetConnectionInterceptor");
                return target.toBytecode();
            }
        });
    }
}
