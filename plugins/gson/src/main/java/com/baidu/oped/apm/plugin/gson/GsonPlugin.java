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
package com.baidu.oped.apm.plugin.gson;

import java.security.ProtectionDomain;

import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentMethod;
import com.baidu.oped.apm.bootstrap.instrument.MethodFilters;
import com.baidu.oped.apm.bootstrap.instrument.Instrumentor;
import com.baidu.oped.apm.bootstrap.instrument.transformer.TransformCallback;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPlugin;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPluginSetupContext;
import com.baidu.oped.apm.common.trace.AnnotationKey;
import com.baidu.oped.apm.common.trace.AnnotationKeyFactory;
import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.common.trace.ServiceTypeFactory;

/**
 * @author ChaYoung You
 */
public class GsonPlugin implements ProfilerPlugin {
    public static final ServiceType GSON_SERVICE_TYPE = ServiceTypeFactory.of(5010, "GSON");
    public static final AnnotationKey GSON_ANNOTATION_KEY_JSON_LENGTH = AnnotationKeyFactory.of(9000, "gson.json.length");

    private static final String GSON_GROUP = "GSON_GROUP";

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("com.google.gson.Gson", new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor pluginContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = pluginContext.getInstrumentClass(loader, className, classfileBuffer);

                for (InstrumentMethod m : target.getDeclaredMethods(MethodFilters.name("fromJson"))) {
                    m.addGroupedInterceptor("com.baidu.oped.apm.plugin.gson.interceptor.FromJsonInterceptor", GSON_GROUP);
                }
                
                for (InstrumentMethod m : target.getDeclaredMethods(MethodFilters.name("toJson"))) {
                    m.addGroupedInterceptor("com.baidu.oped.apm.plugin.gson.interceptor.ToJsonInterceptor", GSON_GROUP);
                }
                
                return target.toBytecode();
            }
        });
    }
}
