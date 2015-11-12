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
package com.baidu.oped.apm.profiler.plugin.xml.interceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.MethodFilter;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.TargetConstructor;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.TargetConstructors;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.TargetFilter;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.TargetMethod;
import com.baidu.oped.apm.bootstrap.interceptor.annotation.TargetMethods;
import com.baidu.oped.apm.bootstrap.interceptor.group.ExecutionPolicy;
import com.baidu.oped.apm.bootstrap.plugin.ObjectRecipe;
import com.baidu.oped.apm.exception.PinpointException;
import com.baidu.oped.apm.profiler.objectfactory.AutoBindingObjectFactory;
import com.baidu.oped.apm.profiler.objectfactory.InterceptorArgumentProvider;
import com.baidu.oped.apm.profiler.plugin.DefaultProfilerPluginContext;
import com.baidu.oped.apm.profiler.plugin.xml.transformer.ClassCookBook;
import com.baidu.oped.apm.profiler.plugin.xml.transformer.ClassRecipe;
import com.baidu.oped.apm.profiler.plugin.xml.transformer.ConstructorTransformer;
import com.baidu.oped.apm.profiler.plugin.xml.transformer.DedicatedMethodTransformer;
import com.baidu.oped.apm.profiler.plugin.xml.transformer.FilteringMethodTransformer;
import com.baidu.oped.apm.profiler.plugin.xml.transformer.MethodRecipe;
import com.baidu.oped.apm.profiler.plugin.xml.transformer.MethodTransformer;

/**
 * @author Jongho Moon
 *
 */

public class TargetAnnotatedInterceptorInjector implements ClassRecipe {
    private final DefaultProfilerPluginContext pluginContext;
    private final String interceptorClassName;
    private final Object[] providedArguments;
    
    private final String groupName;
    private final ExecutionPolicy executionPoint;


    public TargetAnnotatedInterceptorInjector(DefaultProfilerPluginContext pluginContext, String interceptorClassName, Object[] providedArguments, String groupName, ExecutionPolicy executionPoint) {
        this.pluginContext = pluginContext;
        this.interceptorClassName = interceptorClassName;
        this.providedArguments = providedArguments;
        this.groupName = groupName;
        this.executionPoint = executionPoint;
    }

    @Override
    public void edit(ClassLoader classLoader, InstrumentClass target) throws Throwable {
        Class<? extends Interceptor> interceptorType = pluginContext.injectClass(classLoader, interceptorClassName);
        
        AnnotatedInterceptorInjector injector = new AnnotatedInterceptorInjector(pluginContext, interceptorClassName, providedArguments, groupName, executionPoint);
        ClassRecipe recipe = createMethodEditor(classLoader, interceptorType, target,  injector);
        
        recipe.edit(classLoader, target);
    }
    
    private ClassRecipe createMethodEditor(ClassLoader classLoader, Class<?> interceptorType, InstrumentClass targetClass, AnnotatedInterceptorInjector injector) {
        List<MethodTransformer> editors = new ArrayList<MethodTransformer>();
        
        TargetMethods targetMethods = interceptorType.getAnnotation(TargetMethods.class);
        if (targetMethods != null) {
            for (TargetMethod m : targetMethods.value()) {
                editors.add(createDedicatedMethodEditor(m, injector));
            }
        }

        TargetConstructors targetConstructors = interceptorType.getAnnotation(TargetConstructors.class);
        if (targetConstructors != null) {
            for (TargetConstructor c : targetConstructors.value()) {
                editors.add(createConstructorEditor(c, injector));
            }
        }

        TargetMethod targetMethod = interceptorType.getAnnotation(TargetMethod.class);
        if (targetMethod != null) {
            editors.add(createDedicatedMethodEditor(targetMethod, injector));
        }
        
        TargetConstructor targetConstructor = interceptorType.getAnnotation(TargetConstructor.class);
        if (targetConstructor != null) {
            editors.add(createConstructorEditor(targetConstructor, injector));
        }
        
        TargetFilter targetFilter = interceptorType.getAnnotation(TargetFilter.class);
        if (targetFilter != null) {
            editors.add(createFilteredMethodEditor(targetFilter, targetClass, injector, classLoader));
        }
        
        if (editors.isEmpty()) {
            throw new PinpointException("No target is specified. At least one of @Targets, @TargetMethod, @TargetConstructor, @TargetFilter must present. interceptor: " + interceptorClassName);
        }
        
        return editors.size() == 1 ? editors.get(0) : new ClassCookBook(editors);
    }
    
    private MethodTransformer createDedicatedMethodEditor(TargetMethod annotation, AnnotatedInterceptorInjector injector) {
        String methodName = annotation.name();
        
        if (methodName == null) {
            throw new PinpointException("name() of @TargetMethod cannot be null: " + interceptorClassName);
        }
        
        String[] parameterTypeNames = annotation.paramTypes();
        
        return new DedicatedMethodTransformer(methodName, parameterTypeNames, Arrays.<MethodRecipe>asList(injector), null, false);
    }
    
    private MethodTransformer createConstructorEditor(TargetConstructor annotation, AnnotatedInterceptorInjector injector) {
        String[] parameterTypeNames = annotation.value();
        return new ConstructorTransformer(parameterTypeNames, Arrays.<MethodRecipe>asList(injector), null, false);
    }
    
    private MethodTransformer createFilteredMethodEditor(TargetFilter annotation, InstrumentClass targetClass, AnnotatedInterceptorInjector injector, ClassLoader classLoader) {
        String type = annotation.type();
        
        if (type == null) {
            throw new PinpointException("type of @TargetFilter is null: " + interceptorClassName);
        }
        
        AutoBindingObjectFactory filterFactory = new AutoBindingObjectFactory(pluginContext, classLoader, new InterceptorArgumentProvider(pluginContext.getTraceContext(), targetClass));
        MethodFilter filter = (MethodFilter)filterFactory.createInstance(ObjectRecipe.byConstructor(type, (Object[])annotation.constructorArguments()));
        MethodRecipe recipe = annotation.singleton() ? new SharedAnnotatedInterceptorInjector(injector) : injector;
        
        return new FilteringMethodTransformer(new MethodFilter[] { filter }, Arrays.<MethodRecipe>asList(recipe), null);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TargetAnnotatedInterceptorInjector [interceptorClass=");
        builder.append(interceptorClassName);
        
        if (providedArguments != null) {
            builder.append(", constructorArguments=");
            builder.append(Arrays.toString(providedArguments));
        }
        
        if (groupName != null) {
            builder.append(", group=");
            builder.append(groupName);
            builder.append(", executionPolicy=");
            builder.append(executionPoint);
        }
        
        builder.append(']');
        return builder.toString();
    }
}
