package com.baidu.oped.apm.plugin.tomcat;

import com.baidu.oped.apm.bootstrap.context.Trace;

public interface TraceAccessor {
    void _$APM$_setTrace(Trace trace);
    Trace _$APM$_getTrace();
}