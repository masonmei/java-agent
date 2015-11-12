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
package com.baidu.oped.apm.plugin.jdbc.cubrid;

import java.security.ProtectionDomain;

import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.Instrumentor;
import com.baidu.oped.apm.bootstrap.instrument.transformer.TransformCallback;
import com.baidu.oped.apm.bootstrap.interceptor.group.ExecutionPolicy;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPlugin;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPluginSetupContext;

import static com.baidu.oped.apm.common.util.VarArgs.va;

/**
 * @author Jongho Moon
 *
 */
public class CubridPlugin implements ProfilerPlugin {

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        CubridConfig config = new CubridConfig(context.getConfig());
        
        addCUBRIDConnectionTransformer(context, config);
        addCUBRIDDriverTransformer(context);
        addCUBRIDPreparedStatementTransformer(context, config);
        addCUBRIDStatementTransformer(context);
    }

    
    private void addCUBRIDConnectionTransformer(ProfilerPluginSetupContext setupContext, final CubridConfig config) {
        setupContext.addClassFileTransformer("cubrid.jdbc.driver.CUBRIDConnection", new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                target.addField("com.baidu.oped.apm.bootstrap.plugin.jdbc.DatabaseInfoAccessor");

                target.addGroupedInterceptor("com.baidu.oped.apm.bootstrap.plugin.jdbc.interceptor.ConnectionCloseInterceptor", CubridConstants.GROUP_CUBRID);
                target.addGroupedInterceptor("com.baidu.oped.apm.bootstrap.plugin.jdbc.interceptor.StatementCreateInterceptor", CubridConstants.GROUP_CUBRID);
                target.addGroupedInterceptor("com.baidu.oped.apm.bootstrap.plugin.jdbc.interceptor.PreparedStatementCreateInterceptor", CubridConstants.GROUP_CUBRID);
                
                if (config.isProfileSetAutoCommit()) {
                    target.addGroupedInterceptor("com.baidu.oped.apm.bootstrap.plugin.jdbc.interceptor.TransactionSetAutoCommitInterceptor", CubridConstants.GROUP_CUBRID);
                }
                
                if (config.isProfileCommit()) {
                    target.addGroupedInterceptor("com.baidu.oped.apm.bootstrap.plugin.jdbc.interceptor.TransactionCommitInterceptor", CubridConstants.GROUP_CUBRID);
                }
                
                if (config.isProfileRollback()) {
                    target.addGroupedInterceptor("com.baidu.oped.apm.bootstrap.plugin.jdbc.interceptor.TransactionRollbackInterceptor", CubridConstants.GROUP_CUBRID);
                }
                
                return target.toBytecode();
            }
        });
    }
    
    private void addCUBRIDDriverTransformer(ProfilerPluginSetupContext setupContext) {
        setupContext.addClassFileTransformer("cubrid.jdbc.driver.CUBRIDDriver", new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);

                target.addGroupedInterceptor("com.baidu.oped.apm.bootstrap.plugin.jdbc.interceptor.DriverConnectInterceptor", va(new CubridJdbcUrlParser()), CubridConstants.GROUP_CUBRID, ExecutionPolicy.ALWAYS);
                
                return target.toBytecode();
            }
        });
    }
    
    private void addCUBRIDPreparedStatementTransformer(ProfilerPluginSetupContext setupContext, final CubridConfig config) {
        setupContext.addClassFileTransformer("cubrid.jdbc.driver.CUBRIDPreparedStatement", new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                target.addField("com.baidu.oped.apm.bootstrap.plugin.jdbc.DatabaseInfoAccessor");
                target.addField("com.baidu.oped.apm.bootstrap.plugin.jdbc.ParsingResultAccessor");
                target.addField("com.baidu.oped.apm.bootstrap.plugin.jdbc.BindValueAccessor", "new java.util.HashMap()");
                
                int maxBindValueSize = config.getMaxSqlBindValueSize();

                target.addGroupedInterceptor("com.baidu.oped.apm.bootstrap.plugin.jdbc.interceptor.PreparedStatementExecuteQueryInterceptor", va(maxBindValueSize), CubridConstants.GROUP_CUBRID);
                target.addGroupedInterceptor("com.baidu.oped.apm.bootstrap.plugin.jdbc.interceptor.PreparedStatementBindVariableInterceptor", CubridConstants.GROUP_CUBRID);
                
                return target.toBytecode();
            }
        });
    }
    
    private void addCUBRIDStatementTransformer(ProfilerPluginSetupContext setupContext) {
        setupContext.addClassFileTransformer("cubrid.jdbc.driver.CUBRIDStatement", new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                target.addField("com.baidu.oped.apm.bootstrap.plugin.jdbc.DatabaseInfoAccessor");
                
                target.addGroupedInterceptor("com.baidu.oped.apm.bootstrap.plugin.jdbc.interceptor.StatementExecuteQueryInterceptor", CubridConstants.GROUP_CUBRID);
                target.addGroupedInterceptor("com.baidu.oped.apm.bootstrap.plugin.jdbc.interceptor.StatementExecuteUpdateInterceptor", CubridConstants.GROUP_CUBRID);
                
                return target.toBytecode();
            }
        });
    }
}
