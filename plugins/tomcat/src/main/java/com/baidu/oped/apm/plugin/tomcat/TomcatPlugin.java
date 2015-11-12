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

import java.security.ProtectionDomain;

import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentMethod;
import com.baidu.oped.apm.bootstrap.instrument.MethodFilters;
import com.baidu.oped.apm.bootstrap.instrument.Instrumentor;
import com.baidu.oped.apm.bootstrap.instrument.transformer.TransformCallback;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPlugin;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPluginSetupContext;

import static com.baidu.oped.apm.common.util.VarArgs.va;

/**
 * @author Jongho Moon
 * @author jaehong.kim
 *
 */
public class TomcatPlugin implements ProfilerPlugin {

    /*
     * (non-Javadoc)
     * 
     * @see com.baidu.oped.apm.bootstrap.plugin.ProfilerPlugin#setUp(com.baidu.oped.apm.bootstrap.plugin.ProfilerPluginSetupContext)
     */
    @Override
    public void setup(ProfilerPluginSetupContext context) {
        context.addApplicationTypeDetector(new TomcatDetector());

        TomcatConfiguration config = new TomcatConfiguration(context.getConfig());

        if (config.isTomcatHidePinpointHeader()) {
            addRequestFacadeEditor(context);
        }

        addRequestEditor(context);
        addStandardHostValveEditor(context, config);
        addStandardServiceEditor(context);
        addTomcatConnectorEditor(context);
        addWebappLoaderEditor(context);

        addAsyncContextImpl(context);
    }

    private void addRequestEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("org.apache.catalina.connector.Request", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(classLoader, className, classfileBuffer);
                target.addField(TomcatConstants.METADATA_TRACE);
                target.addField(TomcatConstants.METADATA_ASYNC);

                // clear request.
                InstrumentMethod recycleMethodEditorBuilder = target.getDeclaredMethod("recycle");
                if (recycleMethodEditorBuilder != null) {
                    recycleMethodEditorBuilder.addInterceptor("com.baidu.oped.apm.plugin.tomcat.interceptor.RequestRecycleInterceptor");
                }

                // trace asynchronous process.
                InstrumentMethod startAsyncMethodEditor = target.getDeclaredMethod("startAsync", "javax.servlet.ServletRequest", "javax.servlet.ServletResponse");
                if (startAsyncMethodEditor != null) {
                    startAsyncMethodEditor.addInterceptor("com.baidu.oped.apm.plugin.tomcat.interceptor.RequestStartAsyncInterceptor");
                }

                return target.toBytecode();
            }
        });
    }

    private void addRequestFacadeEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("org.apache.catalina.connector.RequestFacade", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(classLoader, className, classfileBuffer);
                if (target != null) {
                    target.weave("com.baidu.oped.apm.plugin.tomcat.aspect.RequestFacadeAspect");
                    return target.toBytecode();
                }

                return null;
            }
        });
    }

    private void addStandardHostValveEditor(ProfilerPluginSetupContext context, final TomcatConfiguration config) {
        context.addClassFileTransformer("org.apache.catalina.core.StandardHostValve", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(classLoader, className, classfileBuffer);

                InstrumentMethod method = target.getDeclaredMethod("invoke", "org.apache.catalina.connector.Request", "org.apache.catalina.connector.Response");
                if (method != null) {
                    method.addInterceptor("com.baidu.oped.apm.plugin.tomcat.interceptor.StandardHostValveInvokeInterceptor", va(config.getTomcatExcludeUrlFilter()));
                }

                return target.toBytecode();
            }
        });
    }

    private void addStandardServiceEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("org.apache.catalina.core.StandardService", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(classLoader, className, classfileBuffer);

                // Tomcat 6
                InstrumentMethod startEditor = target.getDeclaredMethod("start");
                if (startEditor != null) {
                    startEditor.addInterceptor("com.baidu.oped.apm.plugin.tomcat.interceptor.StandardServiceStartInterceptor");
                }

                // Tomcat 7
                InstrumentMethod startInternalEditor = target.getDeclaredMethod("startInternal");
                if (startInternalEditor != null) {
                    startInternalEditor.addInterceptor("com.baidu.oped.apm.plugin.tomcat.interceptor.StandardServiceStartInterceptor");
                }

                return target.toBytecode();
            }
        });
    }

    private void addTomcatConnectorEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("org.apache.catalina.connector.Connector", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(classLoader, className, classfileBuffer);

                // Tomcat 6
                InstrumentMethod initializeEditor = target.getDeclaredMethod("initialize");
                if (initializeEditor != null) {
                    initializeEditor.addInterceptor("com.baidu.oped.apm.plugin.tomcat.interceptor.ConnectorInitializeInterceptor");
                }

                // Tomcat 7
                InstrumentMethod initInternalEditor = target.getDeclaredMethod("initInternal");
                if (initInternalEditor != null) {
                    initInternalEditor.addInterceptor("com.baidu.oped.apm.plugin.tomcat.interceptor.ConnectorInitializeInterceptor");
                }

                return target.toBytecode();
            }
        });
    }

    private void addWebappLoaderEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("org.apache.catalina.loader.WebappLoader", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(classLoader, className, classfileBuffer);

                // Tomcat 6 - org.apache.catalina.loader.WebappLoader.start()
                InstrumentMethod startEditor = target.getDeclaredMethod("start");
                if (startEditor != null) {
                    startEditor.addInterceptor("com.baidu.oped.apm.plugin.tomcat.interceptor.WebappLoaderStartInterceptor");
                }

                // Tomcat 7, 8 - org.apache.catalina.loader.WebappLoader.startInternal()
                InstrumentMethod startInternalEditor = target.getDeclaredMethod("startInternal");
                if (startInternalEditor != null) {
                    startInternalEditor.addInterceptor("com.baidu.oped.apm.plugin.tomcat.interceptor.WebappLoaderStartInterceptor");
                }

                return target.toBytecode();
            }
        });
    }

    private void addAsyncContextImpl(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("org.apache.catalina.core.AsyncContextImpl", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(classLoader, className, classfileBuffer);
                target.addField(TomcatConstants.METADATA_ASYNC_TRACE_ID);

                for (InstrumentMethod method : target.getDeclaredMethods(MethodFilters.name("dispatch"))) {
                    method.addInterceptor("com.baidu.oped.apm.plugin.tomcat.interceptor.AsyncContextImplDispatchMethodInterceptor");
                }

                return target.toBytecode();
            }
        });
    }
}
