package com.baidu.oped.apm.bootstrap.context;

import com.baidu.oped.apm.common.trace.AnnotationKey;
import com.baidu.oped.apm.common.trace.ServiceType;

public interface SpanEventRecorder extends FrameAttachment {

    void recordTime(boolean time);
    
    void recordException(Throwable throwable);

    void recordApi(MethodDescriptor methodDescriptor);

    void recordApi(MethodDescriptor methodDescriptor, Object[] args);

    void recordApi(MethodDescriptor methodDescriptor, Object args, int index);

    void recordApi(MethodDescriptor methodDescriptor, Object[] args, int start, int end);

    void recordApiCachedString(MethodDescriptor methodDescriptor, String args, int index);

    ParsingResult recordSqlInfo(String sql);

    void recordSqlParsingResult(ParsingResult parsingResult);

    void recordSqlParsingResult(ParsingResult parsingResult, String bindValue);

    void recordAttribute(AnnotationKey key, String value);

    void recordAttribute(AnnotationKey key, int value);

    void recordAttribute(AnnotationKey key, Object value);

    void recordServiceType(ServiceType serviceType);

    void recordRpcName(String rpc);

    void recordDestinationId(String destinationId);

    void recordEndPoint(String endPoint);

    void recordNextSpanId(long spanId);

    void recordAsyncId(int asyncId);
    
    void recordNextAsyncId(int asyncId);
    
    void recordAsyncSequence(short sequence);
}