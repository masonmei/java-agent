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

import com.baidu.oped.apm.bootstrap.context.Trace;
import com.baidu.oped.apm.profiler.context.storage.SpanStorage;
import com.baidu.oped.apm.profiler.sender.EnhancedDataSender;
import com.baidu.oped.apm.profiler.sender.LoggingDataSender;
import com.baidu.oped.apm.rpc.FutureListener;
import com.baidu.oped.apm.rpc.ResponseMessage;
import com.baidu.oped.apm.rpc.client.ApmClientReconnectEventListener;
import com.baidu.oped.apm.test.TestAgentInformation;

import org.apache.thrift.TBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author emeroad
 */
public class TraceTest {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void trace() {
        DefaultTraceId traceId = new DefaultTraceId("agent", 0, 1);
        DefaultTraceContext defaultTraceContext = getDefaultTraceContext();
        DefaultTrace trace = new DefaultTrace(defaultTraceContext, traceId, 0L, true);
        trace.setStorage(new SpanStorage(LoggingDataSender.DEFAULT_LOGGING_DATA_SENDER));
        trace.traceBlockBegin();

        // get data form db
        getDataFromDB(trace);

        // response to client

        trace.traceBlockEnd();
    }


    @Test
    public void popEventTest() {
        DefaultTraceId traceId = new DefaultTraceId("agent", 0, 1);
        DefaultTraceContext defaultTraceContext = getDefaultTraceContext();
        DefaultTrace trace = new DefaultTrace(defaultTraceContext, traceId, 0L, true);
        TestDataSender dataSender = new TestDataSender();
        trace.setStorage(new SpanStorage(LoggingDataSender.DEFAULT_LOGGING_DATA_SENDER));
        trace.close();

        logger.info(String.valueOf(dataSender.event));
    }

    private DefaultTraceContext getDefaultTraceContext() {
        DefaultTraceContext defaultTraceContext = new DefaultTraceContext(new TestAgentInformation());
        return defaultTraceContext;
    }

    public class TestDataSender implements EnhancedDataSender {
        public boolean event;

        @Override
        public boolean send(TBase<?, ?> data) {
            event = true;
            return false;
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean request(TBase<?, ?> data) {
            return send(data);
        }

        @Override
        public boolean request(TBase<?, ?> data, int retry) {
            return send(data);
        }

        @Override
        public boolean request(TBase<?, ?> data, FutureListener<ResponseMessage> listener) {
            return send(data);
        }

        @Override
        public boolean addReconnectEventListener(ApmClientReconnectEventListener eventListener) {
            return false;
        }

        @Override
        public boolean removeReconnectEventListener(ApmClientReconnectEventListener eventListener) {
            return false;
        }

        @Override
        public boolean isNetworkAvailable() {
            return true;
        }
    }

    private void getDataFromDB(Trace trace) {
        trace.traceBlockBegin();

        // db server request
        // get a db response
        trace.traceBlockEnd();
    }
}
