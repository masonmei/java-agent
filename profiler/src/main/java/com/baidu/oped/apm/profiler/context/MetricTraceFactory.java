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

package com.baidu.oped.apm.profiler.context;

import com.baidu.oped.apm.bootstrap.context.*;
import com.baidu.oped.apm.common.trace.HistogramSchema;
import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.profiler.monitor.metric.ContextMetric;
import com.baidu.oped.apm.profiler.monitor.metric.MetricRegistry;

/**
 * @author emeroad
 */
public class MetricTraceFactory implements TraceFactory, TraceFactoryWrapper {
    private final TraceFactory delegate;
    private final MetricRegistry metricRegistry;

    private MetricTraceFactory(TraceFactory traceFactory, ServiceType serviceType) {
        if (traceFactory == null) {
            throw new NullPointerException("traceFactory must not be null");
        }
        if (serviceType == null) {
            throw new NullPointerException("serviceType must not be null");
        }
        this.delegate = traceFactory;
        this.metricRegistry = new MetricRegistry(serviceType);
    }

    public static TraceFactory wrap(TraceFactory traceFactory, ServiceType serviceType) {
        return new MetricTraceFactory(traceFactory, serviceType);
    }

    @Override
    public TraceFactory unwrap() {
        final TraceFactory copy = this.delegate;
        if (copy instanceof TraceFactoryWrapper) {
            return ((TraceFactoryWrapper) copy).unwrap();
        }
        return copy;
    }

    @Override
    public Trace currentTraceObject() {
        return delegate.currentTraceObject();
    }

    @Override
    public Trace currentRpcTraceObject() {
        return delegate.currentRpcTraceObject();
    }

    @Override
    public Trace currentRawTraceObject() {
        return delegate.currentRawTraceObject();
    }

    @Override
    public Trace disableSampling() {
        return delegate.disableSampling();
    }

    @Override
    public Trace continueTraceObject(TraceId traceID) {
        return delegate.continueTraceObject(traceID);
    }

    @Override
    public Trace continueTraceObject(Trace trace) {
        return delegate.continueTraceObject(trace);
    }

    @Override
    public Trace continueAsyncTraceObject(AsyncTraceId traceId, int asyncId, long startTime) {
        return delegate.continueAsyncTraceObject(traceId, asyncId, startTime);
    }

    @Override
    public Trace newTraceObject() {
        return delegate.newTraceObject();
    }

    @Override
    public Trace newTraceObject(TraceType traceType) {
        return delegate.newTraceObject(traceType);
    }

    @Override
    public Trace removeTraceObject() {
        final Trace trace = delegate.removeTraceObject();
//        TODO;
//        long time = trace.getSpanRecorder().getResponseTime();
//        metricRegistry.addResponseTime(time);
        return trace;
    }

    public Metric getRpcMetric(ServiceType serviceType) {
        if (serviceType == null) {
            throw new NullPointerException("serviceType must not be null");
        }

        return this.metricRegistry.getRpcMetric(serviceType);
    }


    public void recordContextMetricIsError() {
        recordContextMetric(HistogramSchema.ERROR_SLOT_TIME);
    }

    public void recordContextMetric(int elapsedTime) {
        final ContextMetric contextMetric = this.metricRegistry.getResponseMetric();
        contextMetric.addResponseTime(elapsedTime);
    }

    public void recordAcceptResponseTime(String parentApplicationName, short parentApplicationType, int elapsedTime) {
        final ContextMetric contextMetric = this.metricRegistry.getResponseMetric();
        contextMetric.addAcceptHistogram(parentApplicationName, parentApplicationType, elapsedTime);
    }

    public void recordUserAcceptResponseTime(int elapsedTime) {
        final ContextMetric contextMetric = this.metricRegistry.getResponseMetric();
        contextMetric.addUserAcceptHistogram(elapsedTime);
    }
}
