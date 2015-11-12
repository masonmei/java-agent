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

package com.baidu.oped.apm.profiler.plugin;

import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;
import com.baidu.oped.apm.bootstrap.instrument.transformer.TransformCallback;
import com.baidu.oped.apm.bootstrap.plugin.ApplicationTypeDetector;
import com.baidu.oped.apm.bootstrap.plugin.ProfilerPluginSetupContext;

/**
 * @author emeroad
 */
public class GuardProfilerPluginContext implements ProfilerPluginSetupContext {

    private final ProfilerPluginSetupContext delegate;
    private boolean close = false;

    public GuardProfilerPluginContext(ProfilerPluginSetupContext delegate) {
        if (delegate == null) {
            throw new NullPointerException("delegate must not be null");
        }
        this.delegate = delegate;
    }



    @Override
    public ProfilerConfig getConfig() {
//        checkOpen();
        return this.delegate.getConfig();
    }

    @Override
    public void addApplicationTypeDetector(ApplicationTypeDetector... detectors) {
        checkOpen();
        this.delegate.addApplicationTypeDetector(detectors);
    }

    @Override
    public void addClassFileTransformer(String targetClassName, TransformCallback transformCallback) {
        if (targetClassName == null) {
            throw new NullPointerException("targetClassName must not be null");
        }
        if (transformCallback == null) {
            throw new NullPointerException("transformCallback must not be null");
        }
        checkOpen();
        this.delegate.addClassFileTransformer(targetClassName, transformCallback);
    }

    private void checkOpen() {
        if (close) {
            throw new IllegalStateException("ProfilerPluginSetupContext already initialized");
        }
    }

    public void close() {
        this.close = true;
    }
}
