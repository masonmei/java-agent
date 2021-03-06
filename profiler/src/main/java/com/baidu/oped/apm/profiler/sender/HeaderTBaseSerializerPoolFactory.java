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

package com.baidu.oped.apm.profiler.sender;

import com.baidu.oped.apm.profiler.util.ObjectPoolFactory;
import com.baidu.oped.apm.thrift.io.HeaderTBaseSerializer;
import com.baidu.oped.apm.thrift.io.HeaderTBaseSerializerFactory;

/**
 * @author Taejin Koo
 */
public class HeaderTBaseSerializerPoolFactory implements ObjectPoolFactory<HeaderTBaseSerializer> {

    private final HeaderTBaseSerializerFactory serializerFactory;

    public HeaderTBaseSerializerPoolFactory(boolean safetyGuaranteed, int outputStreamSize, boolean autoExpand) {
        this.serializerFactory = new HeaderTBaseSerializerFactory(false, outputStreamSize, true);
    }

    @Override
    public HeaderTBaseSerializer create() {
        return serializerFactory.createSerializer();
    }

    @Override
    public void beforeReturn(HeaderTBaseSerializer t) {
        t.reset();
    }

}
