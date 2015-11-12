package com.baidu.oped.apm.bootstrap.context;

public interface AsyncTraceId extends TraceId {

    int getAsyncId();
    
    long getSpanStartTime();
    
    TraceId getParentTraceId();
    
    short nextAsyncSequence();
}
