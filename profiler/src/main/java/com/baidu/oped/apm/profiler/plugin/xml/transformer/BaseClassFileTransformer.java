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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPluginSetupContext;
import com.baidu.oped.apm.exception.ApmException;

/**
 * @author Jongho Moon
 *
 */
public abstract class BaseClassFileTransformer implements ClassFileTransformer {
    private final ProfilerPluginSetupContext pluginContext;

    public BaseClassFileTransformer(ProfilerPluginSetupContext pluginContext) {
        this.pluginContext = pluginContext;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
           return transform(pluginContext, loader, className, classBeingRedefined, protectionDomain, classfileBuffer); 
        } catch (InstrumentException e) {
            throw new ApmException(e);
        }
    } 
    
    protected abstract byte[] transform(ProfilerPluginSetupContext context, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException;
    
}
