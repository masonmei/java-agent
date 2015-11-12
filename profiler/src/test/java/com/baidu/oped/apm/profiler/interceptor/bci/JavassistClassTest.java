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

package com.baidu.oped.apm.profiler.interceptor.bci;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import com.baidu.oped.apm.bootstrap.config.DefaultProfilerConfig;
import javassist.bytecode.Descriptor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;
import com.baidu.oped.apm.bootstrap.context.DatabaseInfo;
import com.baidu.oped.apm.bootstrap.instrument.ClassFilters;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentMethod;
import com.baidu.oped.apm.bootstrap.instrument.Instrumentor;
import com.baidu.oped.apm.bootstrap.instrument.transformer.TransformCallback;
import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.bootstrap.plugin.jdbc.UnKnownDatabaseInfo;
import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.profiler.DefaultAgent;
import com.baidu.oped.apm.profiler.instrument.JavassistClassPool;
import com.baidu.oped.apm.profiler.interceptor.registry.GlobalInterceptorRegistryBinder;
import com.baidu.oped.apm.profiler.logging.Slf4jLoggerBinder;
import com.baidu.oped.apm.test.MockAgent;
import com.baidu.oped.apm.test.TestClassLoader;

/**
 * @author emeroad
 */
public class JavassistClassTest {
    private Logger logger = LoggerFactory.getLogger(JavassistClassTest.class.getName());
    
    @Before
    public void clear() {
        TestInterceptors.clear();
    }

    @Test
    public void testClassHierarchy() throws InstrumentException {
        JavassistClassPool pool = new JavassistClassPool(new GlobalInterceptorRegistryBinder(), null);

        String testObjectName = "com.baidu.oped.apm.profiler.interceptor.bci.TestObject";

        // final CallLoader loader = null; // systemClassLoader
        // final ClassLoader loader = ClassLoader.getSystemClassLoader();
        InstrumentClass testObject = pool.getClass(null, testObjectName, null);

        Assert.assertEquals(testObject.getName(), testObjectName);

        String testObjectSuperClass = testObject.getSuperClass();
        Assert.assertEquals("java.lang.Object", testObjectSuperClass);

        String[] testObjectSuperClassInterfaces = testObject.getInterfaces();
        Assert.assertEquals(testObjectSuperClassInterfaces.length, 0);

        InstrumentClass classHierarchyObject = pool.getClass(null, "com.baidu.oped.apm.profiler.interceptor.bci.ClassHierarchyTestMock", null);
        String hierarchySuperClass = classHierarchyObject.getSuperClass();
        Assert.assertEquals("java.util.HashMap", hierarchySuperClass);

        String[] hierarchyInterfaces = classHierarchyObject.getInterfaces();
        Assert.assertEquals(hierarchyInterfaces.length, 2);
        Assert.assertEquals(hierarchyInterfaces[0], "java.lang.Runnable");
        Assert.assertEquals(hierarchyInterfaces[1], "java.lang.Comparable");
    }

    @Test
    public void testDeclaredMethod() throws InstrumentException {

        JavassistClassPool pool = new JavassistClassPool(new GlobalInterceptorRegistryBinder(), null);

        String testObjectName = "com.baidu.oped.apm.profiler.interceptor.bci.TestObject";

        InstrumentClass testObject = pool.getClass(null, testObjectName, null);

        Assert.assertEquals(testObject.getName(), testObjectName);

        InstrumentMethod declaredMethod = testObject.getDeclaredMethod("callA", null);
        Assert.assertNotNull(declaredMethod);

    }

    @Test
    public void testDeclaredMethods() throws InstrumentException {

        JavassistClassPool pool = new JavassistClassPool(new GlobalInterceptorRegistryBinder(), null);

        String testObjectName = "com.baidu.oped.apm.profiler.interceptor.bci.TestObject";

        InstrumentClass testObject = pool.getClass(null, testObjectName, null);
        Assert.assertEquals(testObject.getName(), testObjectName);

        int findMethodCount = 0;
        for (InstrumentMethod methodInfo : testObject.getDeclaredMethods()) {
            if (!methodInfo.getName().equals("callA")) {
                continue;
            }
            String[] parameterTypes = methodInfo.getParameterTypes();
            if (parameterTypes == null || parameterTypes.length == 0) {
                findMethodCount++;
            }
        }
        Assert.assertEquals(findMethodCount, 1);
    }

    @Test
    public void addTraceValue() throws Exception {
        final TestClassLoader loader = getTestClassLoader();
        final String javassistClassName = "com.baidu.oped.apm.profiler.interceptor.bci.TestObject";

        loader.addTransformer(javassistClassName, new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                try {
                    logger.info("modify cl:{}", loader);

                    InstrumentClass aClass = instrumentContext.getInstrumentClass(loader, javassistClassName, classfileBuffer);

                    aClass.addField(ObjectTraceValue.class.getName());
                    aClass.addField(IntTraceValue.class.getName());
                    aClass.addField(DatabaseInfoTraceValue.class.getName());
                    aClass.addField(BindValueTraceValue.class.getName());

                    String methodName = "callA";
                    aClass.getDeclaredMethod(methodName).addInterceptor("com.baidu.oped.apm.profiler.interceptor.TestBeforeInterceptor");
                    return aClass.toBytecode();
                } catch (InstrumentException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        });
        
        loader.initialize();

        Class<?> testObjectClazz = loader.loadClass(javassistClassName);
        final String methodName = "callA";
        logger.info("class:{}", testObjectClazz.toString());
        final Object testObject = testObjectClazz.newInstance();
        Method callA = testObjectClazz.getMethod(methodName);
        callA.invoke(testObject);
        
        Class<?> objectTraceValue = loader.loadClass(ObjectTraceValue.class.getName());
        Assert.assertTrue("ObjectTraceValue implements fail", objectTraceValue.isInstance(testObject));
        objectTraceValue.getMethod("_$PINPOINT$_setTraceObject", Object.class).invoke(testObject, "a");
        Object get = objectTraceValue.getMethod("_$PINPOINT$_getTraceObject").invoke(testObject);
        Assert.assertEquals("a", get);
        

        Class<?> intTraceValue = loader.loadClass(IntTraceValue.class.getName());
        Assert.assertTrue("IntTraceValue implements fail", intTraceValue.isInstance(testObject));
        intTraceValue.getMethod("_$PINPOINT$_setTraceInt", int.class).invoke(testObject, 1);
        int a = (Integer)intTraceValue.getMethod("_$PINPOINT$_getTraceInt").invoke(testObject);
        Assert.assertEquals(1, a);

        
        Class<?> databaseTraceValue = loader.loadClass(DatabaseInfoTraceValue.class.getName());
        Assert.assertTrue("DatabaseInfoTraceValue implements fail", databaseTraceValue.isInstance(testObject));
        databaseTraceValue.getMethod("_$PINPOINT$_setTraceDatabaseInfo", DatabaseInfo.class).invoke(testObject, UnKnownDatabaseInfo.INSTANCE);
        Object databaseInfo = databaseTraceValue.getMethod("_$PINPOINT$_getTraceDatabaseInfo").invoke(testObject);
        Assert.assertSame(UnKnownDatabaseInfo.INSTANCE, databaseInfo);
    }

    @Test
    public void testBeforeAddInterceptor() throws Exception {
        final TestClassLoader loader = getTestClassLoader();
        final String javassistClassName = "com.baidu.oped.apm.profiler.interceptor.bci.TestObject";

        loader.addTransformer(javassistClassName, new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                try {
                    logger.info("modify className:{} cl:{}", className, classLoader);

                    InstrumentClass aClass = instrumentContext.getInstrumentClass(classLoader, javassistClassName, classfileBuffer);
                    
                    String methodName = "callA";
                    aClass.getDeclaredMethod(methodName).addInterceptor("com.baidu.oped.apm.profiler.interceptor.TestBeforeInterceptor");
                    
                    return aClass.toBytecode();
                } catch (InstrumentException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        });
        
        loader.initialize();

        Class<?> testObjectClazz = loader.loadClass(javassistClassName);
        final String methodName = "callA";
        logger.info("class:{}", testObjectClazz.toString());
        final Object testObject = testObjectClazz.newInstance();
        Method callA = testObjectClazz.getMethod(methodName);
        callA.invoke(testObject);
        Interceptor interceptor = getInterceptor(loader, 0);
        assertEqualsIntField(interceptor, "call", 1);
        assertEqualsObjectField(interceptor, "className", "com.baidu.oped.apm.profiler.interceptor.bci.TestObject");
        assertEqualsObjectField(interceptor, "methodName", methodName);
        assertEqualsObjectField(interceptor, "args", null);

        assertEqualsObjectField(interceptor, "target", testObject);

    }

    private Interceptor getInterceptor(final TestClassLoader loader, int index) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        Interceptor interceptor = (Interceptor)loader.loadClass("com.baidu.oped.apm.profiler.interceptor.bci.TestInterceptors").getMethod("get", int.class).invoke(null, index);
        return interceptor;
    }

    private TestClassLoader getTestClassLoader() {
        PLoggerFactory.initialize(new Slf4jLoggerBinder());

        ProfilerConfig profilerConfig = new DefaultProfilerConfig();
        profilerConfig.setApplicationServerType(ServiceType.TEST_STAND_ALONE.getName());
        DefaultAgent agent = MockAgent.of(profilerConfig);

        return new TestClassLoader(agent);
    }

    public void assertEqualsIntField(Object target, String fieldName, int value) throws NoSuchFieldException, IllegalAccessException {
        Field field = target.getClass().getField(fieldName);
        int anInt = field.getInt(target);
        Assert.assertEquals(anInt, value);
    }

    public void assertEqualsObjectField(Object target, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = target.getClass().getField(fieldName);
        Object obj = field.get(target);
        Assert.assertEquals(value, obj);
    }

    @Test
    public void testAddAfterInterceptor() throws Exception {

        final TestClassLoader loader = getTestClassLoader();
        final String testClassObject = "com.baidu.oped.apm.profiler.interceptor.bci.TestObject2";
        
        loader.addTransformer(testClassObject, new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                try {
                    logger.info("modify cl:{}", classLoader);
                    InstrumentClass aClass = instrumentContext.getInstrumentClass(classLoader, testClassObject, classfileBuffer);
                    
                    String methodName = "callA";
                    aClass.getDeclaredMethod(methodName).addInterceptor("com.baidu.oped.apm.profiler.interceptor.TestAfterInterceptor");
                    
                    String methodName2 = "callB";
                    aClass.getDeclaredMethod(methodName2).addInterceptor("com.baidu.oped.apm.profiler.interceptor.TestAfterInterceptor");
                    
                    return aClass.toBytecode();
                } catch (InstrumentException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        });
        
        loader.initialize();

        Class<?> testObjectClazz = loader.loadClass(testClassObject);
        final String methodName = "callA";
        logger.info("class:{}", testObjectClazz.toString());
        final Object testObject = testObjectClazz.newInstance();
        Method callA = testObjectClazz.getMethod(methodName);
        Object result = callA.invoke(testObject);

        Interceptor interceptor = getInterceptor(loader, 0);
        assertEqualsIntField(interceptor, "call", 1);
        assertEqualsObjectField(interceptor, "className", testClassObject);
        assertEqualsObjectField(interceptor, "methodName", methodName);
        assertEqualsObjectField(interceptor, "args", null);

        assertEqualsObjectField(interceptor, "target", testObject);
        assertEqualsObjectField(interceptor, "result", result);

        final String methodName2 = "callB";
        Method callBMethod = testObject.getClass().getMethod(methodName2);
        callBMethod.invoke(testObject);

        Interceptor interceptor2 = getInterceptor(loader, 1);
        assertEqualsIntField(interceptor2, "call", 1);
        assertEqualsObjectField(interceptor2, "className", testClassObject);
        assertEqualsObjectField(interceptor2, "methodName", methodName2);
        assertEqualsObjectField(interceptor2, "args", null);

        assertEqualsObjectField(interceptor2, "target", testObject);
        assertEqualsObjectField(interceptor2, "result", null);

    }

    @Test
    public void nullDescriptor() {
        String nullDescriptor = Descriptor.ofParameters(null);
        logger.info("Descript null:{}", nullDescriptor);
    }

    @Test
    public void testAddGetter() throws Exception {
        final TestClassLoader loader = getTestClassLoader();
        final String targetClassName = "com.baidu.oped.apm.profiler.interceptor.bci.TestObject3";
        
        loader.addTransformer(targetClassName, new TransformCallback() {
            
            @Override
            public byte[] doInTransform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                try {
                    logger.info("modify cl:{}", classLoader);
                    InstrumentClass aClass = instrumentContext.getInstrumentClass(classLoader, className, classfileBuffer);
                    
                    aClass.addGetter(StringGetter.class.getName(), "value");
                    aClass.addGetter(IntGetter.class.getName(), "intValue");
                    
                    return aClass.toBytecode();
                } catch (InstrumentException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        });
        
        loader.initialize();

        Object testObject = loader.loadClass(targetClassName).newInstance();
        
        Class<?> stringGetter = loader.loadClass(StringGetter.class.getName());
        Class<?> intGetter = loader.loadClass(IntGetter.class.getName());
        
        Assert.assertTrue(stringGetter.isInstance(testObject));
        Assert.assertTrue(intGetter.isInstance(testObject));

        String value = "hehe";
        int intValue = 99;

        Method method = testObject.getClass().getMethod("setValue", String.class);
        method.invoke(testObject, value);
        
        Method getString = stringGetter.getMethod("_$PINPOINT$_getString");
        Assert.assertEquals(value, getString.invoke(testObject));

        Method setIntValue = testObject.getClass().getMethod("setIntValue", int.class);
        setIntValue.invoke(testObject, intValue);

        Method getInt = intGetter.getMethod("_$PINPOINT$_getInt");
        Assert.assertEquals(intValue, getInt.invoke(testObject));

    }


    @Test
    public void getNestedClasses() throws Exception {
        JavassistClassPool pool = new JavassistClassPool(new GlobalInterceptorRegistryBinder(), null);
        String testObjectName = "com.baidu.oped.apm.profiler.interceptor.bci.TestObjectNestedClass";
        InstrumentClass testObject = pool.getClass(null, testObjectName, null);
        Assert.assertEquals(testObject.getName(), testObjectName);

        // find class name condition.
        final String targetClassName = "com.baidu.oped.apm.profiler.interceptor.bci.TestObjectNestedClass$InstanceInner";
        for (InstrumentClass c : testObject.getNestedClasses(ClassFilters.name(targetClassName))) {
            assertEquals(targetClassName, c.getName());
        }

        // find enclosing method condition.
        assertEquals(2, testObject.getNestedClasses(ClassFilters.enclosingMethod("annonymousInnerClass")).size());

        // find interface condition.
        assertEquals(2, testObject.getNestedClasses(ClassFilters.interfaze("java.util.concurrent.Callable")).size());

        // find enclosing method & interface condition.
        assertEquals(1, testObject.getNestedClasses(ClassFilters.chain(ClassFilters.enclosingMethod("annonymousInnerClass"), ClassFilters.interfaze("java.util.concurrent.Callable"))).size());
    }
    
    @Test
    public void hasEnclodingMethod() throws Exception {
        JavassistClassPool pool = new JavassistClassPool(new GlobalInterceptorRegistryBinder(), null);
        String testObjectName = "com.baidu.oped.apm.profiler.interceptor.bci.TestObjectNestedClass";
        InstrumentClass testObject = pool.getClass(null, testObjectName, null);
        Assert.assertEquals(testObject.getName(), testObjectName);

        assertEquals(1, testObject.getNestedClasses(ClassFilters.enclosingMethod("enclosingMethod", "java.lang.String", "int")).size());
        assertEquals(0, testObject.getNestedClasses(ClassFilters.enclosingMethod("enclosingMethod", "int")).size());
    }
}
