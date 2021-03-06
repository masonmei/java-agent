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

package com.baidu.oped.apm.profiler.monitor.codahale.gc;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.baidu.oped.apm.profiler.monitor.codahale.MetricMonitorRegistry;
import com.baidu.oped.apm.thrift.dto.TJvmGc;
import com.baidu.oped.apm.thrift.dto.TJvmGcType;

import java.util.SortedMap;

import static com.baidu.oped.apm.profiler.monitor.codahale.MetricMonitorValues.*;

/**
 * HotSpot's Serial collector
 *
 * @author emeroad
 * @author harebox
 */
public class SerialCollector implements GarbageCollector {

    public static final TJvmGcType GC_TYPE = TJvmGcType.SERIAL;

    private final Gauge<Long> heapInit;
    private final Gauge<Long> heapCommitted;
    private final Gauge<Long> heapUsed;
    private final Gauge<Long> heapMax;

    private final Gauge<Long> heapNonHeapInit;
    private final Gauge<Long> heapNonHeapCommitted;
    private final Gauge<Long> heapNonHeapUsed;
    private final Gauge<Long> heapNonHeapMax;

    private final Gauge<Long> gcCount;
    private final Gauge<Long> gcTime;

    public SerialCollector(MetricMonitorRegistry registry) {
        if (registry == null) {
            throw new NullPointerException("registry must not be null");
        }

        final MetricRegistry metricRegistry = registry.getRegistry();
        final SortedMap<String, Gauge> gauges = metricRegistry.getGauges();

        this.heapMax = getLongGauge(gauges, JVM_MEMORY_HEAP_MAX);
        this.heapUsed = getLongGauge(gauges, JVM_MEMORY_HEAP_USED);
        this.heapInit = getLongGauge(gauges, JVM_MEMORY_HEAP_INIT);
        this.heapCommitted = getLongGauge(gauges, JVM_MEMORY_HEAP_COMMITTED);

        this.heapNonHeapMax = getLongGauge(gauges, JVM_MEMORY_NONHEAP_MAX);
        this.heapNonHeapUsed = getLongGauge(gauges, JVM_MEMORY_NONHEAP_USED);
        this.heapNonHeapInit = getLongGauge(gauges, JVM_MEMORY_NONHEAP_INIT);
        this.heapNonHeapCommitted = getLongGauge(gauges, JVM_MEMORY_NONHEAP_COMMITTED);

        this.gcCount = getLongGauge(gauges, JVM_GC_SERIAL_MSC_COUNT);
        this.gcTime = getLongGauge(gauges, JVM_GC_SERIAL_MSC_TIME);
    }

    @Override
    public int getTypeCode() {
        return GC_TYPE.ordinal();
    }

    @Override
    public TJvmGc collect() {

        final TJvmGc gc = new TJvmGc();
        gc.setType(GC_TYPE);
        gc.setJvmMemoryHeapMax(heapMax.getValue());
        gc.setJvmMemoryHeapUsed(heapUsed.getValue());
        gc.setJvmMemoryHeapInit(heapInit.getValue());
        gc.setJvmMemoryHeapCommitted(heapCommitted.getValue());

        gc.setJvmMemoryNonHeapMax(heapNonHeapMax.getValue());
        gc.setJvmMemoryNonHeapUsed(heapNonHeapUsed.getValue());
        gc.setJvmMemoryNonHeapInit(heapNonHeapInit.getValue());
        gc.setJvmMemoryNonHeapCommitted(heapNonHeapCommitted.getValue());

        gc.setJvmGcOldCount(gcCount.getValue());
        gc.setJvmGcOldTime(gcTime.getValue());
        return gc;
    }

    @Override
    public String toString() {
        return "HotSpot's Serial collector";
    }

}
