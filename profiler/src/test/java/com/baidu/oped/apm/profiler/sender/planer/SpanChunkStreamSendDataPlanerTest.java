package com.baidu.oped.apm.profiler.sender.planer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.oped.apm.common.Version;
import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.common.util.JvmUtils;
import com.baidu.oped.apm.common.util.SystemPropertyKey;
import com.baidu.oped.apm.profiler.AgentInformation;
import com.baidu.oped.apm.profiler.context.Span;
import com.baidu.oped.apm.profiler.context.SpanChunk;
import com.baidu.oped.apm.profiler.context.SpanChunkFactory;
import com.baidu.oped.apm.profiler.context.SpanEvent;
import com.baidu.oped.apm.profiler.sender.CompositeSpanStreamData;
import com.baidu.oped.apm.profiler.sender.HeaderTBaseSerializerPoolFactory;
import com.baidu.oped.apm.profiler.sender.SpanStreamSendData;
import com.baidu.oped.apm.profiler.sender.SpanStreamSendDataFactory;
import com.baidu.oped.apm.profiler.sender.SpanStreamSendDataSerializer;
import com.baidu.oped.apm.profiler.util.ObjectPool;
import com.baidu.oped.apm.thrift.dto.TSpanEvent;
import com.baidu.oped.apm.thrift.io.HeaderTBaseDeserializer;
import com.baidu.oped.apm.thrift.io.HeaderTBaseDeserializerFactory;
import com.baidu.oped.apm.thrift.io.HeaderTBaseSerializer;
import com.baidu.oped.apm.thrift.io.HeaderTBaseSerializerFactory;

public class SpanChunkStreamSendDataPlanerTest {

    private static SpanChunkFactory spanChunkFactory;

    private static ObjectPool<HeaderTBaseSerializer> objectPool;

    @BeforeClass
    public static void setUp() {
        AgentInformation agentInformation = new AgentInformation("agentId", "applicationName", 0, 0, "machineName", "127.0.0.1", ServiceType.STAND_ALONE,
                JvmUtils.getSystemProperty(SystemPropertyKey.JAVA_VERSION), Version.VERSION);

        HeaderTBaseSerializerPoolFactory serializerFactory = new HeaderTBaseSerializerPoolFactory(true, 1000, true);
        objectPool = new ObjectPool<HeaderTBaseSerializer>(serializerFactory, 16);

        spanChunkFactory = new SpanChunkFactory(agentInformation);
    }

    @Test
    public void spanChunkStreamSendDataPlanerTest() throws Exception {
        int spanEventSize = 10;

        SpanStreamSendDataSerializer serializer = new SpanStreamSendDataSerializer();

        HeaderTBaseSerializerFactory headerTBaseSerializerFactory = new HeaderTBaseSerializerFactory();

        List<SpanEvent> originalSpanEventList = createSpanEventList(spanEventSize);
        SpanChunk spanChunk = spanChunkFactory.create(originalSpanEventList);

        CompositeSpanStreamData spanData = serializer.serializeSpanChunkStream(headerTBaseSerializerFactory.createSerializer(), spanChunk);
        SpanStreamSendDataFactory factory = new SpanStreamSendDataFactory(100, 50, objectPool);
        List<TSpanEvent> spanEventList = getSpanEventList(spanData, factory);

        spanData = serializer.serializeSpanChunkStream(headerTBaseSerializerFactory.createSerializer(), spanChunk);
        factory = new SpanStreamSendDataFactory(objectPool);
        List<TSpanEvent> spanEventList2 = getSpanEventList(spanData, factory);

        Assert.assertEquals(spanEventSize, spanEventList.size());
        Assert.assertEquals(spanEventSize, spanEventList2.size());
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

    private List<TSpanEvent> getSpanEventList(CompositeSpanStreamData spanData, SpanStreamSendDataFactory factory) throws Exception {
        List<TSpanEvent> spanEventList = new ArrayList<TSpanEvent>();

        SpanChunkStreamSendDataPlaner planer = new SpanChunkStreamSendDataPlaner(spanData, factory);
        Iterator<SpanStreamSendData> iterator = planer.getSendDataIterator();
        while (iterator.hasNext()) {
            SpanStreamSendData data = iterator.next();

            ByteBuffer[] sendBuffers = data.getSendBuffers();
            byte[] relatedBuffer = getSpanRelatedBuffer(sendBuffers);

            List<TSpanEvent> result = deserialize(relatedBuffer);

            for (TSpanEvent spanEvent : result) {
                spanEventList.add(spanEvent);
            }
        }

        return spanEventList;
    }

    private byte[] getSpanRelatedBuffer(ByteBuffer[] buffers) {
        int totalBufferSize = 0;

        for (ByteBuffer buffer : buffers) {
            totalBufferSize += buffer.remaining();
        }

        byte[] relatedBuffer = new byte[totalBufferSize];

        int offset = 0;
        for (ByteBuffer buffer : buffers) {
            int bufferlength = buffer.remaining();

            buffer.get(relatedBuffer, offset, bufferlength);
            offset += bufferlength;
        }

        return relatedBuffer;
    }

    private List<TSpanEvent> deserialize(byte[] data) throws TException {

        ByteBuffer bb = ByteBuffer.wrap(data);

        bb.get();
        bb.get();
        int chunkSize = bb.get();

        List<TSpanEvent> eventList = new ArrayList<TSpanEvent>();

        for (int i = 0; i < chunkSize; i++) {
            short componentSize = bb.getShort();
            byte[] component = new byte[componentSize];
            bb.get(component);

            HeaderTBaseDeserializer deserialize = new HeaderTBaseDeserializerFactory().createDeserializer();
            List<TBase<?, ?>> value = deserialize.deserializeList(component);

            for (int j = 0; j < value.size(); j++) {
                TBase tbase = value.get(j);

                if (tbase instanceof TSpanEvent) {
                    eventList.add((TSpanEvent) tbase);
                } else {
                }
            }

        }
        return eventList;
    }

}
