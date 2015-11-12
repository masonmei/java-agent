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

package com.baidu.oped.apm.plugin.ibatis;

import static com.baidu.oped.apm.common.util.VarArgs.va;

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
public class IBatisPlugin implements ProfilerPlugin {

    public static final ServiceType IBATIS = ServiceTypeFactory.of(5500, "IBATIS");
    public static final ServiceType IBATIS_SPRING = ServiceTypeFactory.of(5501, "IBATIS_SPRING", "IBATIS");

    private static final String IBATIS_SCOPE = "IBATIS_SCOPE";

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        ProfilerConfig profilerConfig = context.getConfig();
        if (profilerConfig.isIBatisEnabled()) {
            addInterceptorsForSqlMapExecutors(context);
            addInterceptorsForSqlMapClientTemplate(context);
        }
    }

    // SqlMapClient / SqlMapSession
    private void addInterceptorsForSqlMapExecutors(ProfilerPluginSetupContext context) {
        final ServiceType serviceType = IBATIS;
        final String[] sqlMapExecutorImplClasses = { "com.ibatis.sqlmap.engine.impl.SqlMapClientImpl",
                "com.ibatis.sqlmap.engine.impl.SqlMapSessionImpl" };
        addInterceptorsForClasses(context, serviceType, sqlMapExecutorImplClasses);
    }

    // SqlMapClientTemplate
    private void addInterceptorsForSqlMapClientTemplate(ProfilerPluginSetupContext context) {
        final ServiceType serviceType = IBATIS_SPRING;
        final String[] sqlMapClientTemplateClasses = { "org.springframework.orm.ibatis.SqlMapClientTemplate" };
        addInterceptorsForClasses(context, serviceType, sqlMapClientTemplateClasses);
    }

    private void addInterceptorsForClasses(ProfilerPluginSetupContext context, ServiceType serviceType,
            String... targetClassNames) {
        final MethodFilter methodFilter = MethodFilters.name("insert", "delete", "update", "queryForList",
                "queryForMap", "queryForObject", "queryForPaginatedList");
        for (String targetClassName : targetClassNames) {
            addInterceptorsForClass(context, targetClassName, serviceType, methodFilter);
        }
    }

    private void addInterceptorsForClass(ProfilerPluginSetupContext context, final String targetClassName,
            final ServiceType serviceType, final MethodFilter methodFilter) {

        context.addClassFileTransformer(targetClassName, new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader loader,
                                        String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                                        byte[] classfileBuffer) throws InstrumentException {

                final InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);

                final List<InstrumentMethod> methodsToTrace = target.getDeclaredMethods(methodFilter);
                for (InstrumentMethod methodToTrace : methodsToTrace) {
                    String sqlMapOperationInterceptor = "com.baidu.oped.apm.plugin.ibatis.interceptor.SqlMapOperationInterceptor";
                    methodToTrace.addGroupedInterceptor(sqlMapOperationInterceptor, va(serviceType), IBATIS_SCOPE, ExecutionPolicy.BOUNDARY
                    );
                }

                return target.toBytecode();
            }

        });
    }
}
