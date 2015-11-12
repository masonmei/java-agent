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

package com.baidu.oped.apm.profiler.instrument.interceptor;

import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.bootstrap.context.TraceContext;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentMethod;
import com.baidu.oped.apm.bootstrap.interceptor.AroundInterceptor0;
import com.baidu.oped.apm.bootstrap.interceptor.AroundInterceptor3;

/**
 * @author emeroad
 */
public class InvokeAfterCodeGeneratorTest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Test
    public void testGenerate_AroundInterceptor3_catchClause() throws Exception {

        final Class<AroundInterceptor3> aroundInterceptor3Class = AroundInterceptor3.class;
        //                                                                           this,          param0,       param1,        param2,        return,        throwable
        final Method interceptorAfter = aroundInterceptor3Class.getMethod("after", Object.class, Object.class, Object.class, Object.class, Object.class, Throwable.class);
        final InstrumentClass mockClass = mock(InstrumentClass.class);
        Mockito.when(mockClass.getName()).thenReturn("TestClass");

        final InstrumentMethod mockMethod = mock(InstrumentMethod.class);
        Mockito.when(mockMethod.getName()).thenReturn("TestMethod");
        Mockito.when(mockMethod.getParameterTypes()).thenReturn(new String[]{"java.lang.Object", "java.lang.Object", "java.lang.Object"});
        Mockito.when(mockMethod.getReturnType()).thenReturn("java.lang.Object");
        
        TraceContext context = mock(TraceContext.class);


        final InvokeAfterCodeGenerator invokeAfterCodeGenerator = new InvokeAfterCodeGenerator(100, aroundInterceptor3Class, interceptorAfter, mockClass, mockMethod, context, false, true);
        final String generate = invokeAfterCodeGenerator.generate();

        logger.debug("testGenerate_AroundInterceptor3_catchClause:{}", generate);
        Assert.assertTrue(generate.contains("($w)$1"));
        Assert.assertTrue(generate.contains("($w)$2"));
        Assert.assertTrue(generate.contains("($w)$3"));

        Assert.assertTrue(generate.contains("$e"));

    }

    @Test
    public void testGenerate_AroundInterceptor3_NoCatchClause() throws Exception {

        final Class<AroundInterceptor3> aroundInterceptor3Class = AroundInterceptor3.class;
        //                                                                           this,          param0,       param1,        param2,        return,        throwable
        final Method interceptorAfter = aroundInterceptor3Class.getMethod("after", Object.class, Object.class, Object.class, Object.class, Object.class, Throwable.class);
        final InstrumentClass mockClass = mock(InstrumentClass.class);
        Mockito.when(mockClass.getName()).thenReturn("TestClass");

        final InstrumentMethod mockMethod = mock(InstrumentMethod.class);
        Mockito.when(mockMethod.getName()).thenReturn("TestMethod");
        Mockito.when(mockMethod.getParameterTypes()).thenReturn(new String[]{"java.lang.Object", "java.lang.Object", "java.lang.Object"});
        Mockito.when(mockMethod.getReturnType()).thenReturn("java.lang.Object");

        TraceContext context = mock(TraceContext.class);

        final InvokeAfterCodeGenerator invokeAfterCodeGenerator = new InvokeAfterCodeGenerator(100, aroundInterceptor3Class, interceptorAfter, mockClass, mockMethod, context, false, false);
        final String generate = invokeAfterCodeGenerator.generate();

        logger.debug("testGenerate_AroundInterceptor3_NoCatchClause:{}", generate);
        Assert.assertTrue(generate.contains("($w)$1"));
        Assert.assertTrue(generate.contains("($w)$2"));
        Assert.assertTrue(generate.contains("($w)$3"));

        Assert.assertTrue(generate.contains("($w)$_"));

    }

    @Test
    public void testGenerate_AroundInterceptor3_methodParam2() throws Exception {

        final Class<AroundInterceptor3> aroundInterceptor3Class = AroundInterceptor3.class;
        //                                                                           this,          param0,       param1,        param2,        return,        throwable
        final Method interceptorAfter = aroundInterceptor3Class.getMethod("after", Object.class, Object.class, Object.class, Object.class, Object.class, Throwable.class);
        final InstrumentClass mockClass = mock(InstrumentClass.class);
        Mockito.when(mockClass.getName()).thenReturn("TestClass");

        final InstrumentMethod mockMethod = mock(InstrumentMethod.class);
        Mockito.when(mockMethod.getName()).thenReturn("TestMethod");
        Mockito.when(mockMethod.getParameterTypes()).thenReturn(new String[]{"java.lang.Object", "java.lang.Object"});
        Mockito.when(mockMethod.getReturnType()).thenReturn("java.lang.Object");
        
        TraceContext context = mock(TraceContext.class);


        final InvokeAfterCodeGenerator invokeAfterCodeGenerator = new InvokeAfterCodeGenerator(100, aroundInterceptor3Class, interceptorAfter, mockClass, mockMethod, context, false, true);
        final String generate = invokeAfterCodeGenerator.generate();

        logger.debug("testGenerate_AroundInterceptor3_methodParam2:{}", generate);
        Assert.assertTrue(generate.contains("($w)$1"));
        Assert.assertTrue(generate.contains("($w)$2"));
        Assert.assertFalse(generate.contains("($w)$3"));

        Assert.assertTrue(generate.contains("$e"));

    }

    @Test
    public void testGenerate_AroundInterceptor3_methodParam4() throws Exception {

        final Class<AroundInterceptor3> aroundInterceptor3Class = AroundInterceptor3.class;
        //                                                                           this,          param0,       param1,        param2,        return,        throwable
        final Method interceptorAfter = aroundInterceptor3Class.getMethod("after", Object.class, Object.class, Object.class, Object.class, Object.class, Throwable.class);
        final InstrumentClass mockClass = mock(InstrumentClass.class);
        Mockito.when(mockClass.getName()).thenReturn("TestClass");

        final InstrumentMethod mockMethod = mock(InstrumentMethod.class);
        Mockito.when(mockMethod.getName()).thenReturn("TestMethod");
        Mockito.when(mockMethod.getParameterTypes()).thenReturn(new String[]{"java.lang.Object", "java.lang.Object", "java.lang.Object", "java.lang.Object"});
        Mockito.when(mockMethod.getReturnType()).thenReturn("java.lang.Object");

        TraceContext context = mock(TraceContext.class);

        final InvokeAfterCodeGenerator invokeAfterCodeGenerator = new InvokeAfterCodeGenerator(100, aroundInterceptor3Class, interceptorAfter, mockClass, mockMethod, context, false, true);
        final String generate = invokeAfterCodeGenerator.generate();

        logger.debug("testGenerate_AroundInterceptor3_methodParam4:{}", generate);
        Assert.assertTrue(generate.contains("($w)$1"));
        Assert.assertTrue(generate.contains("($w)$2"));
        Assert.assertTrue(generate.contains("($w)$3"));
        Assert.assertFalse(generate.contains("($w)$4"));

        Assert.assertTrue(generate.contains("$e"));

    }


    @Test
    public void testGenerate_AroundInterceptor0() throws Exception {

        final Class<AroundInterceptor0> aroundInterceptor3Class = AroundInterceptor0.class;
        //                                                                           this,          return,        throwable
        final Method interceptorAfter = aroundInterceptor3Class.getMethod("after", Object.class, Object.class, Throwable.class);
        final InstrumentClass mockClass = mock(InstrumentClass.class);
        Mockito.when(mockClass.getName()).thenReturn("TestClass");

        final InstrumentMethod mockMethod = mock(InstrumentMethod.class);
        Mockito.when(mockMethod.getName()).thenReturn("TestMethod");
        Mockito.when(mockMethod.getParameterTypes()).thenReturn(new String[]{});
        Mockito.when(mockMethod.getReturnType()).thenReturn("java.lang.Object");

        TraceContext context = mock(TraceContext.class);

        final InvokeAfterCodeGenerator invokeAfterCodeGenerator = new InvokeAfterCodeGenerator(100, aroundInterceptor3Class, interceptorAfter, mockClass, mockMethod, context, false, true);
        final String generate = invokeAfterCodeGenerator.generate();

        logger.debug("testGenerate_AroundInterceptor0:{}", generate);
        Assert.assertFalse(generate.contains("($w)$1"));
        Assert.assertFalse(generate.contains("($w)$2"));
        Assert.assertFalse(generate.contains("($w)$3"));

        Assert.assertTrue(generate.contains("$e"));

    }
}