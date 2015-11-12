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
package com.baidu.oped.apm.plugin.google.httpclient;

import java.security.ProtectionDomain;

import com.baidu.oped.apm.bootstrap.async.AsyncTraceIdAccessor;
import com.baidu.oped.apm.bootstrap.instrument.ClassFilters;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentMethod;
import com.baidu.oped.apm.bootstrap.instrument.Instrumentor;
import com.baidu.oped.apm.bootstrap.instrument.transformer.TransformCallback;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPlugin;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPluginSetupContext;

/**
 * @author jaehong.kim
 *
 */
public class HttpClientPlugin implements ProfilerPlugin {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        final HttpClientPluginConfig config = new HttpClientPluginConfig(context.getConfig());
        logger.debug("[GoogleHttpClient] Initialized config={}", config);

        logger.debug("[GoogleHttpClient] Add HttpRequest class.");
        addHttpRequestClass(context, config);
    }

    private void addHttpRequestClass(ProfilerPluginSetupContext context, final HttpClientPluginConfig config) {
        context.addClassFileTransformer("com.google.api.client.http.HttpRequest", new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                InstrumentMethod execute = target.getDeclaredMethod("execute", new String[] {});
                if (execute != null) {
                    execute.addInterceptor("com.baidu.oped.apm.plugin.google.httpclient.interceptor.HttpRequestExecuteMethodInterceptor");
                }
                
                if (config.isAsync()) {
                    InstrumentMethod executeAsync = target.getDeclaredMethod("executeAsync", "java.util.concurrent.Executor");
                    if (executeAsync != null) {
                        executeAsync.addInterceptor("com.baidu.oped.apm.plugin.google.httpclient.interceptor.HttpRequestExecuteAsyncMethodInterceptor");
                    }

                    for(InstrumentClass nestedClass : target.getNestedClasses(ClassFilters.chain(ClassFilters.enclosingMethod("executeAsync", "java.util.concurrent.Executor"), ClassFilters.interfaze("java.util.concurrent.Callable")))) {
                        logger.debug("Find nested class {}", target.getName());
                        instrumentContext.addClassFileTransformer(loader, nestedClass.getName(), new TransformCallback() {
                            @Override
                            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                                target.addField(AsyncTraceIdAccessor.class.getName());

                                InstrumentMethod constructor = target.getConstructor("com.google.api.client.http.HttpRequest");
                                if(constructor != null) {
                                    logger.debug("Add constuctor interceptor for nested class {}", target.getName());
                                    constructor.addInterceptor("com.baidu.oped.apm.plugin.google.httpclient.interceptor.HttpRequestExecuteAsyncMethodInnerClassConstructorInterceptor");
                                }

                                InstrumentMethod m = target.getDeclaredMethod("call");
                                if(m != null) {
                                    logger.debug("Add method interceptor for nested class {}.{}", target.getName(), m.getName());
                                    m.addInterceptor("com.baidu.oped.apm.plugin.google.httpclient.interceptor.HttpRequestExecuteAsyncMethodInnerClassCallMethodInterceptor");
                                }

                                return target.toBytecode();
                            }
                        });
                    }
                }
                        
                return target.toBytecode();
            }
        });
    }
}