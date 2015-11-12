package com.baidu.oped.apm.bootstrap.interceptor;

import com.baidu.oped.apm.bootstrap.async.AsyncTraceIdAccessor;
import com.baidu.oped.apm.bootstrap.context.AsyncTraceId;
import com.baidu.oped.apm.bootstrap.context.MethodDescriptor;
import com.baidu.oped.apm.bootstrap.context.SpanEventRecorder;
import com.baidu.oped.apm.bootstrap.context.Trace;
import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.logging.PLogger;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.common.trace.ServiceType;

public abstract class SpanAsyncEventSimpleAroundInterceptor implements AroundInterceptor {
    protected final PLogger logger = PLoggerFactory.getLogger(getClass());
    protected final boolean isDebug = logger.isDebugEnabled();

    protected final MethodDescriptor methodDescriptor;
    protected final TraceContext traceContext;
    final MethodDescriptor asyncMethodDescriptor = new AsyncMethodDescriptor();

    public SpanAsyncEventSimpleAroundInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor) {
        this.traceContext = traceContext;
        this.methodDescriptor = methodDescriptor;

        traceContext.cacheApi(asyncMethodDescriptor);
    }
    
    private AsyncTraceId getAsyncTraceId(Object target) {
        return target instanceof AsyncTraceIdAccessor ? ((AsyncTraceIdAccessor)target)._$PINPOINT$_getAsyncTraceId() : null;
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, methodDescriptor.getClassName(), methodDescriptor.getMethodName(), "", args);
        }

        final AsyncTraceId asyncTraceId = getAsyncTraceId(target);
        
        if (asyncTraceId == null) {
            logger.debug("Not found asynchronous invocation metadata");
            return;
        }

        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            trace = traceContext.continueAsyncTraceObject(asyncTraceId, asyncTraceId.getAsyncId(), asyncTraceId.getSpanStartTime());
            if (trace == null) {
                logger.warn("Failed to continue async trace. 'result is null'");
                return;
            }
            if(isDebug) {
                logger.debug("Continue async trace. {}", asyncTraceId);
            }

            traceFirstBlockBegin(trace);
        }

        try {
            final SpanEventRecorder recorder = trace.traceBlockBegin();
            doInBeforeTrace(recorder, asyncTraceId, target, args);
        } catch (Throwable th) {
            if (logger.isWarnEnabled()) {
                logger.warn("BEFORE. Caused:{}", th.getMessage(), th);
            }
        }
    }

    private void traceFirstBlockBegin(final Trace trace) {
        // first block
        final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
        recorder.recordServiceType(ServiceType.ASYNC);
        recorder.recordApi(asyncMethodDescriptor);
    }
    

    protected abstract void doInBeforeTrace(SpanEventRecorder recorder, AsyncTraceId asyncTraceId, Object target, Object[] args);

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, methodDescriptor.getClassName(), methodDescriptor.getMethodName(), "", args, result, throwable);
        }

        if (getAsyncTraceId(target) == null) {
            logger.debug("Not found asynchronous invocation metadata");
            return;
        }
        
        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        try {
            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            doInAfterTrace(recorder, target, args, result, throwable);
        } catch (Throwable th) {
            if (logger.isWarnEnabled()) {
                logger.warn("AFTER error. Caused:{}", th.getMessage(), th);
            }
        } finally {
            trace.traceBlockEnd();
            if (trace.isAsync() && trace.isRootStack()) {
                if(isDebug) {
                    logger.debug("Close async trace. {}");
                }

                traceFirstBlockEnd(trace);
                trace.close();
                traceContext.removeTraceObject();
            }
        }
    }

    private void traceFirstBlockEnd(final Trace trace) {
    }

    protected abstract void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable);

    public class AsyncMethodDescriptor implements MethodDescriptor {

        private int apiId = 0;

        @Override
        public String getMethodName() {
            return "";
        }

        @Override
        public String getClassName() {
            return "";
        }

        @Override
        public String[] getParameterTypes() {
            return null;
        }

        @Override
        public String[] getParameterVariableName() {
            return null;
        }

        @Override
        public String getParameterDescriptor() {
            return "";
        }

        @Override
        public int getLineNumber() {
            return -1;
        }

        @Override
        public String getFullName() {
            return AsyncMethodDescriptor.class.getName();
        }

        @Override
        public void setApiId(int apiId) {
            this.apiId = apiId;
        }

        @Override
        public int getApiId() {
            return apiId;
        }

        @Override
        public String getApiDescriptor() {
            return "Asynchronous Invocation";
        }

        @Override
        public int getType() {
            return 200;
        }
    }
}