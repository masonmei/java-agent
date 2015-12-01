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

import java.io.Closeable;
import java.io.IOException;

import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.bootstrap.logging.PLoggerBinder;
import com.baidu.oped.apm.bootstrap.logging.PLoggerFactory;
import com.baidu.oped.apm.profiler.logging.Slf4jLoggerBinder;
import com.baidu.oped.apm.test.MockAgent;
import com.baidu.oped.apm.test.TestClassLoader;
import com.baidu.oped.apm.test.TestClassLoaderFactory;

/**
 * @author hyungil.jeong
 * @author emeroad
 */
public class TestContext implements Closeable {

    private static final String BASE_TEST_CLASS_NAME = "com.baidu.oped.apm.test.junit4.BaseApmTest";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final PLoggerBinder loggerBinder = new Slf4jLoggerBinder();
    private final TestClassLoader classLoader;
    private final MockAgent mockAgent;

    private final Class<?> baseTestClass;


    public TestContext() {
        this.mockAgent = createMockAgent();
        this.classLoader = TestClassLoaderFactory.createTestClassLoader(mockAgent);
        this.classLoader.initialize();
        try {
            this.baseTestClass = classLoader.loadClass(BASE_TEST_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    private MockAgent createMockAgent() {
        logger.trace("agent create");
        return MockAgent.of("apm.config");
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public MockAgent getMockAgent() {
        return mockAgent;
    }

    public TestClass createTestClass(Class<?> testClass) {
        try {
            final Class<?> testClazz = classLoader.loadClass(testClass.getName());
            return new TestClass(testClazz);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public Class<?> getBaseTestClass() {
        return this.baseTestClass;
    }

    @Override
    public void close() throws IOException {
        if (mockAgent != null) {
            mockAgent.stop();
        }
        PLoggerFactory.unregister(loggerBinder);
    }
}
