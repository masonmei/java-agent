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

import com.baidu.oped.apm.bootstrap.instrument.GuardInstrumentor;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.Instrumentor;
import com.baidu.oped.apm.bootstrap.instrument.matcher.Matcher;
import com.baidu.oped.apm.bootstrap.instrument.transformer.TransformCallback;
import com.baidu.oped.apm.exception.PinpointException;
import com.baidu.oped.apm.profiler.plugin.xml.transformer.MatchableClassFileTransformer;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * @author emeroad
 */
public class MatchableClassFileTransformerGuardDelegate implements MatchableClassFileTransformer {

    private final Instrumentor instrumentor;
    private final Matcher matcher;
    private final TransformCallback transformCallback;


    public MatchableClassFileTransformerGuardDelegate(Instrumentor instrumentor, Matcher matcher, TransformCallback transformCallback) {
        if (instrumentor == null) {
            throw new NullPointerException("instrumentor must not be null");
        }
        if (matcher == null) {
            throw new NullPointerException("matcher must not be null");
        }
        if (transformCallback == null) {
            throw new NullPointerException("transformCallback must not be null");
        }
        this.instrumentor = instrumentor;
        this.matcher = matcher;
        this.transformCallback = transformCallback;
    }


    @Override
    public Matcher getMatcher() {
        return matcher;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className == null) {
            throw new NullPointerException("className must not be null");
        }

        final GuardInstrumentor guard = new GuardInstrumentor(this.instrumentor);
        try {
            // WARN external plugin api
            return transformCallback.doInTransform(guard, loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
        } catch (InstrumentException e) {
            throw new PinpointException(e);
        } finally {
            guard.close();
        }
    }
}
