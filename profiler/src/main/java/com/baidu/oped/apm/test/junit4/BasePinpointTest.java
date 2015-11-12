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

package com.baidu.oped.apm.test.junit4;

import java.util.ArrayList;
import java.util.List;

import com.baidu.oped.apm.profiler.sender.DataSender;
import com.baidu.oped.apm.test.ListenableDataSender;
import com.baidu.oped.apm.test.MockAgent;
import com.baidu.oped.apm.test.ResettableServerMetaDataHolder;
import com.baidu.oped.apm.test.TestableServerMetaDataListener;

import org.apache.thrift.TBase;
import org.junit.runner.RunWith;

import com.baidu.oped.apm.bootstrap.context.ServerMetaData;
import com.baidu.oped.apm.bootstrap.context.ServerMetaDataHolder;
import com.baidu.oped.apm.common.bo.SpanBo;
import com.baidu.oped.apm.common.bo.SpanEventBo;
import com.baidu.oped.apm.profiler.context.Span;
import com.baidu.oped.apm.profiler.context.SpanEvent;
import com.baidu.oped.apm.test.TBaseRecorder;

/**
 * @author hyungil.jeong
 */
@RunWith(value = PinpointJUnit4ClassRunner.class)
public abstract class BasePinpointTest {

    private volatile TBaseRecorder<? extends TBase<?, ?>> tBaseRecorder;
    private volatile ServerMetaDataHolder serverMetaDataHolder;
    private final TestableServerMetaDataListener listener = new TestableServerMetaDataListener();

    protected List<SpanEventBo> getCurrentSpanEvents() {
        List<SpanEventBo> spanEvents = new ArrayList<SpanEventBo>();
        for (TBase<?, ?> span : this.tBaseRecorder) {
            if (span instanceof SpanEvent) {
                SpanEvent spanEvent = (SpanEvent)span;
                spanEvents.add(new SpanEventBo(spanEvent.getSpan(), spanEvent));
            }
        }
        return spanEvents;
    }

    protected List<SpanBo> getCurrentRootSpans() {
        List<SpanBo> rootSpans = new ArrayList<SpanBo>();
        for (TBase<?, ?> span : this.tBaseRecorder) {
            if (span instanceof Span) {
                rootSpans.add(new SpanBo((Span)span));
            }
        }
        return rootSpans;
    }
    
    protected ServerMetaData getServerMetaData() {
        return this.listener.getServerMetaData();
    }

    private void setTBaseRecorder(TBaseRecorder tBaseRecorder) {
        this.tBaseRecorder = tBaseRecorder;
    }
    
    private void setServerMetaDataHolder(ServerMetaDataHolder serverMetaDataHolder) {
        this.serverMetaDataHolder = serverMetaDataHolder;
    }

    public void setup(TestContext testContext) {
        MockAgent mockAgent = testContext.getMockAgent();

        DataSender spanDataSender = mockAgent.getSpanDataSender();
        if (spanDataSender instanceof ListenableDataSender) {
            ListenableDataSender listenableDataSender = (ListenableDataSender) spanDataSender;

            final TBaseRecorder tBaseRecord = new TBaseRecorder();

            listenableDataSender.setListener(new ListenableDataSender.Listener() {
                @Override
                public boolean handleSend(TBase<?, ?> data) {
                    return tBaseRecord.add(data);
                }
            });
            setTBaseRecorder(tBaseRecord);
        }

        ServerMetaDataHolder serverMetaDataHolder = mockAgent.getTraceContext().getServerMetaDataHolder();
        if (serverMetaDataHolder instanceof ResettableServerMetaDataHolder) {
            ResettableServerMetaDataHolder resettableServerMetaDataHolder = (ResettableServerMetaDataHolder) serverMetaDataHolder;
            this.setServerMetaDataHolder(resettableServerMetaDataHolder);
        }
        this.serverMetaDataHolder.addListener(this.listener);
    }
}
