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

package com.baidu.oped.apm.profiler.plugin.xml.transformer;

import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.Instrumentor;

public class ConditionalClassRecipe implements ClassRecipe {
    private final Instrumentor context;
    private final ClassCondition condition;
    private final ClassRecipe delegate;
    
    public ConditionalClassRecipe(Instrumentor context, ClassCondition condition, ClassRecipe delegate) {
        this.context = context;
        this.condition = condition;
        this.delegate = delegate;
    }
    
    @Override
    public void edit(ClassLoader classLoader, InstrumentClass target) throws Throwable {
        if (condition.check(context, classLoader, target)) {
            delegate.edit(classLoader, target);
        }
    }

    @Override
    public String toString() {
        return "ConditionalClassRecipe[condition=" + condition + ", execute=" + delegate + "]";
    }
}
