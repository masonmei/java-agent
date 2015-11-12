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
package com.baidu.oped.apm.bootstrap.interceptor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.baidu.oped.apm.bootstrap.instrument.MethodFilter;

/**
 * Specify the {@link MethodFilter} which will be used to filter the annotated interceptor's target methods.
 * 
 * @author Jongho Moon
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TargetFilter {
    /**
     * Filter type
     */
    public String type();
    
    /**
     * Arguments for specified {@link MethodFilter}'s constructor. 
     */
    public String[] constructorArguments() default {};
    
    public boolean singleton() default false;
}
