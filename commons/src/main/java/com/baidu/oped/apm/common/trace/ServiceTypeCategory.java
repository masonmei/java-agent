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
package com.baidu.oped.apm.common.trace;

/**
 * @author Jongho Moon <jongho.moon@navercorp.com>
 *
 */
public enum ServiceTypeCategory {
    UNDEFINED_CATEGORY((short)-1, (short)-1),
    APM_INTERNAL((short)0, (short)999),
    SERVER((short)1000, (short)1999),
    DATABASE((short)2000, (short)2999),
    LIBRARY((short)5000, (short)7999),
    CACHE_LIBRARY((short)8000, (short)8999, HistogramSchema.FAST_SCHEMA),
    RPC((short)9000, (short)9999);
   
    
    private final short minCode;
    private final short maxCode;
    private HistogramSchema histogramSchema;

    private ServiceTypeCategory(short minCode, short maxCode) {
        this(minCode, maxCode, HistogramSchema.NORMAL_SCHEMA);
    }
    
    private ServiceTypeCategory(short minCode, short maxCode, HistogramSchema histogramSchema) {
        this.minCode = minCode;
        this.maxCode = maxCode;
        if (histogramSchema == null) {
            throw new NullPointerException("histogramSchema must not be null");
        }
        this.histogramSchema = histogramSchema;
    }
    
    public boolean contains(short code) {
        return minCode <= code && code <= maxCode; 
    }
    
    public boolean contains(ServiceType type) {
        return contains(type.getCode());
    }

    public HistogramSchema getHistogramSchema() {
        return histogramSchema;
    }

    public static ServiceTypeCategory findCategory(short code) {
        for (ServiceTypeCategory serviceTypeCategory : ServiceTypeCategory.values()) {
            if (serviceTypeCategory.contains(code)) {
                return serviceTypeCategory;
            }
        }
        throw new IllegalStateException("Unknown Category code:" + code);
    }
}
