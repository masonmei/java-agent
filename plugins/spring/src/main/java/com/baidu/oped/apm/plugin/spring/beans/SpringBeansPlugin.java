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
package com.baidu.oped.apm.plugin.spring.beans;

import java.security.ProtectionDomain;

import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentMethod;
import com.baidu.oped.apm.bootstrap.instrument.Instrumentor;
import com.baidu.oped.apm.bootstrap.instrument.transformer.TransformCallback;
import com.baidu.oped.apm.bootstrap.plugin.ObjectRecipe;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPlugin;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPluginSetupContext;

import static com.baidu.oped.apm.common.util.VarArgs.va;

/**
 * @author Jongho Moon
 *
 */
public class SpringBeansPlugin implements ProfilerPlugin {

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        addAbstractAutowireCapableBeanFactoryTransformer(context);
    }

    private void addAbstractAutowireCapableBeanFactoryTransformer(final ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory", new TransformCallback() {

            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                BeanMethodTransformer beanTransformer = new BeanMethodTransformer();
                ObjectRecipe beanFilterRecipe = ObjectRecipe.byStaticFactory("com.baidu.oped.apm.plugin.spring.beans.interceptor.TargetBeanFilter", "of", context.getConfig());
                
                InstrumentMethod createBeanInstance = target.getDeclaredMethod("createBeanInstance", "java.lang.String", "org.springframework.beans.factory.support.RootBeanDefinition", "java.lang.Object[]");
                createBeanInstance.addInterceptor("com.baidu.oped.apm.plugin.spring.beans.interceptor.CreateBeanInstanceInterceptor", va(beanTransformer, beanFilterRecipe));

                InstrumentMethod postProcessor = target.getDeclaredMethod("applyBeanPostProcessorsBeforeInstantiation", "java.lang.Class", "java.lang.String");
                postProcessor.addInterceptor("com.baidu.oped.apm.plugin.spring.beans.interceptor.PostProcessorInterceptor", va(beanTransformer, beanFilterRecipe));

                return target.toBytecode();
            }
        });

    }

}
