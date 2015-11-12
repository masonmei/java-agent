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
package com.baidu.oped.apm.plugin.arcus;

import java.security.ProtectionDomain;

import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentMethod;
import com.baidu.oped.apm.bootstrap.instrument.MethodFilters;
import com.baidu.oped.apm.bootstrap.instrument.Instrumentor;
import com.baidu.oped.apm.bootstrap.instrument.transformer.TransformCallback;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPlugin;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPluginSetupContext;
import com.baidu.oped.apm.plugin.arcus.filter.ArcusMethodFilter;
import com.baidu.oped.apm.plugin.arcus.filter.FrontCacheMemcachedMethodFilter;

import static com.baidu.oped.apm.common.util.VarArgs.va;

/**
 * 
 * @author jaehong.kim
 *
 */
public class ArcusPlugin implements ProfilerPlugin {
    private static final String ASYNC_TRACE_ID_ACCESSOR = "com.baidu.oped.apm.bootstrap.async.AsyncTraceIdAccessor";
    private static final String OPERATION_ACCESSOR = "com.baidu.oped.apm.plugin.arcus.OperationAccessor";
    private static final String CACHE_KEY_ACCESSOR = "com.baidu.oped.apm.plugin.arcus.CacheKeyAccessor";
    private static final String CACHE_NAME_ACCESSOR = "com.baidu.oped.apm.plugin.arcus.CacheNameAccessor";
    private static final String SERVICE_CODE_ACCESSOR = "com.baidu.oped.apm.plugin.arcus.ServiceCodeAccessor";

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        ArcusPluginConfig config = new ArcusPluginConfig(context.getConfig());

        boolean arcus = config.isArcus();
        boolean memcached = config.isMemcached();

        if (arcus) {
            addArcusClientEditor(context, config);
            addCollectionFutureEditor(context);
            addFrontCacheGetFutureEditor(context);
            addFrontCacheMemcachedClientEditor(context, config);
            addCacheManagerEditor(context);

            // add none operation future. over 1.5.4
            addBTreeStoreGetFutureEditor(context);
            addCollectionGetBulkFutureEditor(context);
            addSMGetFutureFutureEditor(context);
        }

        if (arcus || memcached) {
            addMemcachedClientEditor(context, config);

            addBaseOperationImplEditor(context);
            addGetFutureEditor(context);
            addOperationFutureEditor(context);
            // add none operation future.
            addImmediateFutureEditor(context);
            addBulkGetFutureEditor(context);
        }
    }

    private void addArcusClientEditor(ProfilerPluginSetupContext context, final ArcusPluginConfig config) {
        context.addClassFileTransformer("net.spy.memcached.ArcusClient", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor context, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = context.getInstrumentClass(loader, className, classfileBuffer);

                if (target.hasMethod("addOp", "java.lang.String", "net.spy.memcached.ops.Operation")) {
                    boolean traceKey = config.isArcusKeyTrace();

                    target.addInterceptor("com.baidu.oped.apm.plugin.arcus.interceptor.SetCacheManagerInterceptor");

                    for (InstrumentMethod m : target.getDeclaredMethods(new ArcusMethodFilter())) {
                        try {
                            m.addInterceptor("com.baidu.oped.apm.plugin.arcus.interceptor.ApiInterceptor", va(traceKey));
                        } catch (Exception e) {
                            if (logger.isWarnEnabled()) {
                                logger.warn("Unsupported method " + className + "." + m.getName(), e);
                            }
                        }
                    }

                    return target.toBytecode();
                } else {
                    return null;
                }
            }
        });
    }

    private void addCacheManagerEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("net.spy.memcached.CacheManager", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor context, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = context.getInstrumentClass(loader, className, classfileBuffer);
                target.addField(SERVICE_CODE_ACCESSOR);
                target.addInterceptor("com.baidu.oped.apm.plugin.arcus.interceptor.CacheManagerConstructInterceptor");
                return target.toBytecode();
            }

        });
    }

    private void addBaseOperationImplEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("net.spy.memcached.protocol.BaseOperationImpl", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor context, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = context.getInstrumentClass(loader, className, classfileBuffer);
                target.addField(SERVICE_CODE_ACCESSOR);
                return target.toBytecode();
            }

        });
    }

    private void addFrontCacheGetFutureEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("net.spy.memcached.plugin.FrontCacheGetFuture", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor context, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = context.getInstrumentClass(loader, className, classfileBuffer);

                target.addField(CACHE_NAME_ACCESSOR);
                target.addField(CACHE_KEY_ACCESSOR);
                target.addInterceptor("com.baidu.oped.apm.plugin.arcus.interceptor.FrontCacheGetFutureConstructInterceptor");

                InstrumentMethod get0 = target.getDeclaredMethod("get", new String[] { "long", "java.util.concurrent.TimeUnit" });
                get0.addInterceptor("com.baidu.oped.apm.plugin.arcus.interceptor.FrontCacheGetFutureGetInterceptor");

                InstrumentMethod get1 = target.getDeclaredMethod("get", new String[0]);
                get1.addInterceptor("com.baidu.oped.apm.plugin.arcus.interceptor.FrontCacheGetFutureGetInterceptor");

                return target.toBytecode();
            }

        });
    }

    private void addFrontCacheMemcachedClientEditor(ProfilerPluginSetupContext context, final ArcusPluginConfig config) {
        context.addClassFileTransformer("net.spy.memcached.plugin.FrontCacheMemcachedClient", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor context, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = context.getInstrumentClass(loader, className, classfileBuffer);
                boolean traceKey = config.isMemcachedKeyTrace();

                for (InstrumentMethod m : target.getDeclaredMethods(new FrontCacheMemcachedMethodFilter())) {
                    try {
                        m.addInterceptor("com.baidu.oped.apm.plugin.arcus.interceptor.ApiInterceptor", va(traceKey));
                    } catch (Exception e) {
                        if (logger.isWarnEnabled()) {
                            logger.warn("Unsupported method " + className + "." + m.getName(), e);
                        }
                    }
                }

                return target.toBytecode();
            }

        });
    }

    private void addMemcachedClientEditor(ProfilerPluginSetupContext context, final ArcusPluginConfig config) {
        context.addClassFileTransformer("net.spy.memcached.MemcachedClient", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor context, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = context.getInstrumentClass(loader, className, classfileBuffer);

                if (target.hasDeclaredMethod("addOp", new String[] { "java.lang.String", "net.spy.memcached.ops.Operation" })) {
                    target.addField(SERVICE_CODE_ACCESSOR);
                    target.addInterceptor("com.baidu.oped.apm.plugin.arcus.interceptor.AddOpInterceptor");
                }

                boolean traceKey = config.isMemcachedKeyTrace();

                for (InstrumentMethod m : target.getDeclaredMethods(new FrontCacheMemcachedMethodFilter())) {
                    try {
                        m.addInterceptor("com.baidu.oped.apm.plugin.arcus.interceptor.ApiInterceptor", va(traceKey));
                    } catch (Exception e) {
                        if (logger.isWarnEnabled()) {
                            logger.warn("Unsupported method " + className + "." + m.getName(), e);
                        }
                    }
                }

                return target.toBytecode();
            }

        });
    }

    private static final TransformCallback FUTURE_TRANSFORMER = new TransformCallback() {

        @Override
        public byte[] doInTransform(Instrumentor context, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = context.getInstrumentClass(loader, className, classfileBuffer);

            target.addField(OPERATION_ACCESSOR);
            target.addField(ASYNC_TRACE_ID_ACCESSOR);

            // setOperation
            InstrumentMethod setOperation = target.getDeclaredMethod("setOperation", new String[] { "net.spy.memcached.ops.Operation" });
            if (setOperation != null) {
                setOperation.addInterceptor("com.baidu.oped.apm.plugin.arcus.interceptor.FutureSetOperationInterceptor");
            }

            // cancel, get, set
            for (InstrumentMethod m : target.getDeclaredMethods(MethodFilters.name("cancel", "get", "set", "signalComplete"))) {
                m.addInterceptor("com.baidu.oped.apm.plugin.arcus.interceptor.FutureGetInterceptor");
            }

            return target.toBytecode();
        }
    };

    private static final TransformCallback INTERNAL_FUTURE_TRANSFORMER = new TransformCallback() {

        @Override
        public byte[] doInTransform(Instrumentor context, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = context.getInstrumentClass(loader, className, classfileBuffer);

            target.addField(ASYNC_TRACE_ID_ACCESSOR);
            
            // cancel, get, set
            for (InstrumentMethod m : target.getDeclaredMethods(MethodFilters.name("cancel", "get"))) {
                m.addInterceptor("com.baidu.oped.apm.plugin.arcus.interceptor.FutureInternalMethodInterceptor");
            }

            return target.toBytecode();
        }
    };

    
    private void addCollectionFutureEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("net.spy.memcached.internal.CollectionFuture", FUTURE_TRANSFORMER);
    }

    private void addGetFutureEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("net.spy.memcached.internal.GetFuture", FUTURE_TRANSFORMER);
    }

    private void addOperationFutureEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("net.spy.memcached.internal.OperationFuture", FUTURE_TRANSFORMER);
    }

    private void addImmediateFutureEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("net.spy.memcached.internal.ImmediateFuture", INTERNAL_FUTURE_TRANSFORMER);
    }

    private void addBulkGetFutureEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("net.spy.memcached.internal.BulkGetFuture", INTERNAL_FUTURE_TRANSFORMER);
    }

    private void addBTreeStoreGetFutureEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("net.spy.memcached.internal.BTreeStoreAndGetFuture", INTERNAL_FUTURE_TRANSFORMER);
    }

    private void addCollectionGetBulkFutureEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("net.spy.memcached.internal.CollectionGetBulkFuture", INTERNAL_FUTURE_TRANSFORMER);
    }

    private void addSMGetFutureFutureEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("net.spy.memcached.internal.SMGetFuture", INTERNAL_FUTURE_TRANSFORMER);
    }
}