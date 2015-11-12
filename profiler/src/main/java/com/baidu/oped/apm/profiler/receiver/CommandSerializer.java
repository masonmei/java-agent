/*
 *
 *  * Copyright 2014 NAVER Corp.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 *
 */

package com.baidu.oped.apm.profiler.receiver;

import com.baidu.oped.apm.common.Version;
import com.baidu.oped.apm.thrift.io.*;

/**
 * @Author Taejin Koo
 */
public class CommandSerializer {

    public static final CommandHeaderTBaseSerializerFactory SERIALIZER_FACTORY;
    public static final CommandHeaderTBaseDeserializerFactory DESERIALIZER_FACTORY;

    static {
        SERIALIZER_FACTORY = new CommandHeaderTBaseSerializerFactory(Version.VERSION);
        DESERIALIZER_FACTORY = new CommandHeaderTBaseDeserializerFactory(Version.VERSION);
    }

}