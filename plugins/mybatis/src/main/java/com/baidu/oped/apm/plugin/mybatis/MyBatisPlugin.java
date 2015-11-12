/*
 * Copyright 2015 NAVER Corp.
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

package com.baidu.oped.apm.plugin.mybatis;

import java.security.ProtectionDomain;
import java.util.List;

import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentMethod;
import com.baidu.oped.apm.bootstrap.instrument.MethodFilter;
import com.baidu.oped.apm.bootstrap.instrument.MethodFilters;
import com.baidu.oped.apm.bootstrap.instrument.Instrumentor;
import com.baidu.oped.apm.bootstrap.instrument.transformer.TransformCallback;
import com.baidu.oped.apm.bootstrap.interceptor.group.ExecutionPolicy;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPlugin;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPluginSetupContext;
import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.common.trace.ServiceTypeFactory;

/**
 * @author HyunGil Jeong
 */
public class MyBatisPlugin implements ProfilerPlugin {

    public static final ServiceType MYBATIS = ServiceTypeFactory.of(5510, "MYBATIS");

    private static final String MYBATIS_SCOPE = "MYBATIS_SCOPE";

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        ProfilerConfig profilerConfig = context.getConfig();
        if (profilerConfig.isMyBatisEnabled()) {
            addInterceptorsForSqlSession(context);
        }
    }

    // SqlSession implementations
    private void addInterceptorsForSqlSession(ProfilerPluginSetupContext context) {
        final MethodFilter methodFilter = MethodFilters.name("selectOne", "selectList", "selectMap", "select",
                "insert", "update", "delete");
        final String[] sqlSessionImpls = { "org.apache.ibatis.session.defaults.DefaultSqlSession",
                "org.mybatis.spring.SqlSessionTemplate" };

        for (final String sqlSession : sqlSessionImpls) {
            context.addClassFileTransformer(sqlSession, new TransformCallback() {

                @Override
                public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader loader,
                                            String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                            byte[] classfileBuffer) throws InstrumentException {
                    
                    final InstrumentClass target = instrumentContext.getInstrumentClass(loader, sqlSession, classfileBuffer);

                    final List<InstrumentMethod> methodsToTrace = target.getDeclaredMethods(methodFilter);
                    for (InstrumentMethod methodToTrace : methodsToTrace) {
                        String sqlSessionOperationInterceptor = "com.baidu.oped.apm.plugin.mybatis.interceptor.SqlSessionOperationInterceptor";
                        methodToTrace.addGroupedInterceptor(sqlSessionOperationInterceptor, MYBATIS_SCOPE, ExecutionPolicy.BOUNDARY);
                    }
                    
                    return target.toBytecode();
                }
            });

        }
    }
}
