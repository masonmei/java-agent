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

package com.baidu.oped.apm.profiler.plugin.xml;

import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.exception.ApmException;
import com.baidu.oped.apm.profiler.plugin.xml.FieldInitializationStrategy.ByConstructor;
import com.baidu.oped.apm.profiler.plugin.xml.transformer.ClassRecipe;

public class FieldInjector implements ClassRecipe {
    private final String accessorTypeName;
    private final FieldInitializationStrategy strategy;
    
    
    public FieldInjector(String accessorTypeName) {
        this(accessorTypeName, null);
    }
    
    public FieldInjector(String accessorTypeName, FieldInitializationStrategy strategy) {
        this.accessorTypeName = accessorTypeName;
        this.strategy = strategy;
    }

    @Override
    public void edit(ClassLoader classLoader, InstrumentClass target) throws InstrumentException {
        if (strategy == null) {
            target.addField(accessorTypeName);
        } else {
            if (strategy instanceof ByConstructor) {
                String javaExpression = "new " + ((ByConstructor)strategy).getClassName() + "();";
                target.addField(accessorTypeName, javaExpression);
            } else {
                throw new ApmException("Unsupported strategy: " + strategy);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FieldInjector[accessorType=");
        builder.append(accessorTypeName);
        
        if (strategy != null) {
            builder.append(", initialize=");
            builder.append(strategy);
        }
        
        builder.append(']');
        return builder.toString();
    }
    
    
}
