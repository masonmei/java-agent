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
package com.baidu.oped.apm.profiler.plugin.xml.transformer;

import com.baidu.oped.apm.bootstrap.instrument.MethodFilter;

/**
 * @author Jongho Moon
 *
 */
public interface BaseClassFileTransformerBuilder {

    public void injectGetter(String getterTypeName, String fieldName);

    public void injectField(String accessorTypeName);

    public void injectField(String accessorTypeName, String initialValueType);

    public InterceptorBuilder injectInterceptor(String className, Object... constructorArgs);

    public void weave(String aspectClassName);

    public MethodTransformerBuilder editMethods(MethodFilter... filter);

    public MethodTransformerBuilder editMethod(String name, String... parameterTypeNames);

    public ConstructorTransformerBuilder editConstructor(String... parameterTypeNames);
    
    public void overrideMethodToDelegate(String name, String... paramTypes);

}