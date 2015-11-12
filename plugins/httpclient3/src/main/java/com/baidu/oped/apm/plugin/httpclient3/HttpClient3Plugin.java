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
package com.baidu.oped.apm.plugin.httpclient3;

import java.security.ProtectionDomain;

import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentMethod;
import com.baidu.oped.apm.bootstrap.instrument.Instrumentor;
import com.baidu.oped.apm.bootstrap.instrument.transformer.TransformCallback;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPlugin;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPluginSetupContext;

/**
 * @author netspider
 * @author emeroad
 * @author minwoo.jung
 * @author jaehong.kim
 *
 */
public class HttpClient3Plugin implements ProfilerPlugin {

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        final HttpClient3PluginConfig config = new HttpClient3PluginConfig(context.getConfig());

        // apache http client 3
        addHttpClient3Class(context, config);

        // apache http client 3 retry
        addDefaultHttpMethodRetryHandlerClass(context, config);
        // 3.1.0
        addHttpConnectionClass(context, config);
        addHttpMethodBaseClass(context, config);
    }

    private void addHttpClient3Class(ProfilerPluginSetupContext context, HttpClient3PluginConfig config) {
        context.addClassFileTransformer("org.apache.commons.httpclient.HttpClient", new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                injectHttpClientExecuteMethod(target, "org.apache.commons.httpclient.HttpMethod");
                injectHttpClientExecuteMethod(target, "org.apache.commons.httpclient.HostConfiguration", "org.apache.commons.httpclient.HttpMethod");
                injectHttpClientExecuteMethod(target, "org.apache.commons.httpclient.HostConfiguration", "org.apache.commons.httpclient.HttpMethod", "org.apache.commons.httpclient.HttpState");
                
                return target.toBytecode();
            }

            private void injectHttpClientExecuteMethod(InstrumentClass target, String... parameterTypeNames) throws InstrumentException {
                InstrumentMethod method = target.getDeclaredMethod("executeMethod", parameterTypeNames);
                
                if (method != null) {
                    method.addInterceptor("com.baidu.oped.apm.plugin.httpclient3.interceptor.ExecuteInterceptor");
                }
            }
        });
    }


    
    private void addDefaultHttpMethodRetryHandlerClass(ProfilerPluginSetupContext context, HttpClient3PluginConfig config) {
        context.addClassFileTransformer("org.apache.commons.httpclient.DefaultHttpMethodRetryHandler", new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                InstrumentMethod retryMethod = target.getDeclaredMethod("retryMethod", "org.apache.commons.httpclient.HttpMethod", "java.io.IOException", "int");
                
                if (retryMethod != null) {
                    retryMethod.addInterceptor("com.baidu.oped.apm.plugin.httpclient3.interceptor.RetryMethodInterceptor");
                }
                
                return target.toBytecode();
            }
        });
    }
    
    private void addHttpConnectionClass(ProfilerPluginSetupContext context, HttpClient3PluginConfig config) {
        context.addClassFileTransformer("org.apache.commons.httpclient.HttpConnection", new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                target.addGetter(HostNameGetter.class.getName(), HttpClient3Constants.FIELD_HOST_NAME);
                target.addGetter(PortNumberGetter.class.getName(), HttpClient3Constants.FIELD_PORT_NUMBER);
                target.addGetter(ProxyHostNameGetter.class.getName(), HttpClient3Constants.FIELD_PROXY_HOST_NAME);
                target.addGetter(ProxyPortNumberGetter.class.getName(), HttpClient3Constants.FIELD_PROXY_PORT_NUMBER);

                InstrumentMethod open = target.getDeclaredMethod("open");
                
                if (open != null) {
                    open.addInterceptor("com.baidu.oped.apm.plugin.httpclient3.interceptor.HttpConnectionOpenMethodInterceptor");
                }
                
                return target.toBytecode();
            }
        });
    }
    
    private void addHttpMethodBaseClass(ProfilerPluginSetupContext context, final HttpClient3PluginConfig config) {
        context.addClassFileTransformer("org.apache.commons.httpclient.HttpMethodBase", new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                InstrumentMethod execute = target.getDeclaredMethod("execute", "org.apache.commons.httpclient.HttpState", "org.apache.commons.httpclient.HttpConnection");
                if (execute != null) {
                    execute.addInterceptor("com.baidu.oped.apm.plugin.httpclient3.interceptor.HttpMethodBaseExecuteMethodInterceptor");
                }
                
                if (config.isApacheHttpClient3ProfileIo()) {
                    InstrumentMethod writeRequest = target.getDeclaredMethod("writeRequest", "org.apache.commons.httpclient.HttpState", "org.apache.commons.httpclient.HttpConnection");
                    
                    if (writeRequest != null) {
                        writeRequest.addInterceptor("com.baidu.oped.apm.plugin.httpclient3.interceptor.HttpMethodBaseRequestAndResponseMethodInterceptor");
                    }
                    
                    InstrumentMethod readResponse = target.getDeclaredMethod("readResponse", "org.apache.commons.httpclient.HttpState", "org.apache.commons.httpclient.HttpConnection");
                    
                    if (readResponse != null) {
                        readResponse.addInterceptor("com.baidu.oped.apm.plugin.httpclient3.interceptor.HttpMethodBaseRequestAndResponseMethodInterceptor");
                    }
                }

                return target.toBytecode();
            }
        });
    }
}