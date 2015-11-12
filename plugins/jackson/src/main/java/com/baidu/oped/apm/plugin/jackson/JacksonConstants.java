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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 implied.
 * See the License for the specific language governing permissions
 and
 * limitations under the License.
 */
package com.baidu.oped.apm.plugin.jackson;

import com.baidu.oped.apm.common.trace.AnnotationKey;
import com.baidu.oped.apm.common.trace.AnnotationKeyFactory;
import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.common.trace.ServiceTypeFactory;

/**
 *  * @author Sungkook Kim
 *   *
 *    */
public final class JacksonConstants {
    private JacksonConstants() {
    }

    public static final ServiceType SERVICE_TYPE = ServiceTypeFactory.of(5011, "JACKSON");

    public static final AnnotationKey ANNOTATION_KEY_LENGTH_VALUE = AnnotationKeyFactory.of(9001, "jackson.json.length");
}
