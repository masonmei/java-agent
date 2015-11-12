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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.oped.apm.common.Version;
import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.common.util.JvmUtils;
import com.baidu.oped.apm.common.util.SystemPropertyKey;
import com.baidu.oped.apm.profiler.AgentInformation;
import com.baidu.oped.apm.profiler.context.DefaultTraceId;
import com.baidu.oped.apm.profiler.context.Span;
import com.baidu.oped.apm.profiler.context.SpanChunk;
import com.baidu.oped.apm.profiler.context.SpanChunkFactory;
import com.baidu.oped.apm.profiler.context.SpanEvent;
import com.baidu.oped.apm.thrift.dto.TSpan;
import com.baidu.oped.apm.thrift.dto.TSpanChunk;
import com.baidu.oped.apm.thrift.dto.TSpanEvent;
import com.baidu.oped.apm.thrift.io.HeaderTBaseDeserializer;
import com.baidu.oped.apm.thrift.io.HeaderTBaseDeserializerFactory;
import com.baidu.oped.apm.thrift.io.HeaderTBaseSerializerFactory;

/**
 * @author Taejin Koo
 */
public class SpanStreamSendDataSerializerTest {

    private static SpanChunkFactory spanChunkFactory;

    @BeforeClass
    public static void setUp() {
        AgentInformation agentInformation = new AgentInformation("agentId", "applicationName", 0, 0, "machineName", "127.0.0.1", ServiceType.STAND_ALONE,
                JvmUtils.getSystemProperty(SystemPropertyKey.JAVA_VERSION), Version.VERSION);
        spanChunkFactory = new SpanChunkFactory(agentInformation);
    }

    @Test
    public void spanStreamSendDataSerializerTest1() throws InterruptedException, TException {
        int spanEventSize = 10;

        SpanStreamSendDataSerializer serializer = new SpanStreamSendDataSerializer();

        HeaderTBaseSerializerFactory factory = new HeaderTBaseSerializerFactory();

        SpanChunk spanChunk = spanChunkFactory.create(createSpanEventList(spanEventSize));
        CompositeSpanStreamData spanData = serializer.serializeSpanChunkStream(factory.createSerializer(), spanChunk);

        Assert.assertEquals(spanEventSize + 1, spanData.getComponentsCount());

        HeaderTBaseDeserializer deserializer = new HeaderTBaseDeserializerFactory().createDeserializer();

        for (int i = 0; i < spanData.getComponentsCount(); i++) {
            ByteBuffer byteBuffer = spanData.getByteBuffer(i);

            byte[] readBuffer = new byte[byteBuffer.remaining()];

            byteBuffer.get(readBuffer);

            Object o = deserializer.deserialize(readBuffer);

            if (o == null) {
                Assert.fail();
            }

            if (i < spanEventSize) {
                Assert.assertTrue(o instanceof TSpanEvent);
            } else {
                Assert.assertTrue(o instanceof TSpanChunk);
            }
        }
    }

    @Test
    public void spanStreamSendDataSerializerTest2() throws InterruptedException, TException {
        int spanEventSize = 10;

        SpanStreamSendDataSerializer serializer = new SpanStreamSendDataSerializer();

        HeaderTBaseSerializerFactory factory = new HeaderTBaseSerializerFactory();

        Span span = createSpan(createSpanEventList(spanEventSize));
        CompositeSpanStreamData spanData = serializer.serializeSpanStream(factory.createSerializer(), span);

        Assert.assertEquals(spanEventSize + 1, spanData.getComponentsCount());

        HeaderTBaseDeserializer deserializer = new HeaderTBaseDeserializerFactory().createDeserializer();

        for (int i = 0; i < spanData.getComponentsCount(); i++) {
            ByteBuffer byteBuffer = spanData.getByteBuffer(i);

            byte[] readBuffer = new byte[byteBuffer.remaining()];

            byteBuffer.get(readBuffer);

            Object o = deserializer.deserialize(readBuffer);

            if (o == null) {
                Assert.fail();
            }

            if (i < spanEventSize) {
                Assert.assertTrue(o instanceof TSpanEvent);
            } else {
                Assert.assertTrue(o instanceof TSpan);
            }
        }
    }

    private Span createSpan(List<SpanEvent> spanEventList) {
        DefaultTraceId traceId = new DefaultTraceId("test", 0, 1);
        Span span = new Span();

        for (SpanEvent spanEvent : spanEventList) {
            span.addToSpanEventList(spanEvent);
        }

        span.setAgentId("agentId");
        span.recordTraceId(traceId);
        return span;
    }

    private List<SpanEvent> createSpanEventList(int size) throws InterruptedException {
        Span span = new Span();

        List<SpanEvent> spanEventList = new ArrayList<SpanEvent>(size);
        for (int i = 0; i < size; i++) {
            SpanEvent spanEvent = new SpanEvent(span);
            spanEvent.markStartTime();
            Thread.sleep(1);
            spanEvent.markAfterTime();

            spanEventList.add(spanEvent);
        }

        return spanEventList;
    }

}
