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

package com.baidu.oped.apm.common.util;

import java.util.ArrayList;
import java.util.List;

import com.baidu.oped.apm.common.bo.AnnotationBo;
import com.baidu.oped.apm.common.trace.AnnotationKey;

/**
 * @author emeroad
 */
public final class AnnotationUtils {
    private AnnotationUtils() {
    }

    public static String findApiAnnotation(List<AnnotationBo> list) {
        if (list == null) {
            return null;
        }
        AnnotationBo annotationBo = findAnnotationBo(list, AnnotationKey.API);
        if (annotationBo != null) {
            return (String) annotationBo.getValue();
        }
        return null;
    }
    
    public static String findApiTagAnnotation(List<AnnotationBo> list) {
        if (list == null) {
            return null;
        }
        AnnotationBo annotationBo = findAnnotationBo(list, AnnotationKey.API_TAG);
        if (annotationBo != null) {
            return (String) annotationBo.getValue();
        }
        return null;
    }
    

    public static AnnotationBo findAnnotationBo(List<AnnotationBo> annotationBoList, AnnotationKey annotationKey) {
        for (AnnotationBo annotation : annotationBoList) {
            int key = annotation.getKey();
            if (annotationKey.getCode() == key) {
                return annotation;
            }
        }
        return null;
    }

}
