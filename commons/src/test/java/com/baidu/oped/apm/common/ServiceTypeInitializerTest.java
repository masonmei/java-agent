/**
 * Copyright 2014 NAVER Corp.
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
package com.baidu.oped.apm.common;

import static com.baidu.oped.apm.common.trace.ServiceTypeProperty.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import com.baidu.oped.apm.common.service.*;
import com.baidu.oped.apm.common.trace.AnnotationKey;
import com.baidu.oped.apm.common.trace.AnnotationKeyFactory;
import com.baidu.oped.apm.common.trace.ServiceType;
import com.baidu.oped.apm.common.trace.ServiceTypeFactory;
import com.baidu.oped.apm.common.trace.TraceMetadataLoader;
import com.baidu.oped.apm.common.trace.TraceMetadataProvider;
import com.baidu.oped.apm.common.trace.TraceMetadataSetupContext;
import com.baidu.oped.apm.common.util.StaticFieldLookUp;

import org.junit.Test;

/**
 * @author Jongho Moon <jongho.moon@navercorp.com>
 *
 */
public class ServiceTypeInitializerTest {
    private static final ServiceType[] TEST_TYPES = {
        ServiceTypeFactory.of(1209, "FOR_UNIT_TEST", "UNDEFINED", TERMINAL, RECORD_STATISTICS, INCLUDE_DESTINATION_ID)
    };
    
    private static final AnnotationKey[] TEST_KEYS = {
        AnnotationKeyFactory.of(1209, "Duplicate-API")
    };

    private static final ServiceType[] DUPLICATED_CODE_WITH_DEFAULT_TYPE = {
        ServiceTypeFactory.of(ServiceType.USER.getCode(), "FOR_UNIT_TEST", "UNDEFINED", TERMINAL, RECORD_STATISTICS, INCLUDE_DESTINATION_ID)
    };
    
    private static final ServiceType[] DUPLICATED_NAME_WITH_DEFAULT_TYPE = {
        ServiceTypeFactory.of(1209, ServiceType.USER.getName(), "UNDEFINED", TERMINAL, RECORD_STATISTICS, INCLUDE_DESTINATION_ID)
    };
    
    private static final AnnotationKey[] DUPLICATED_CODE_WITH_DEFAULT_KEY = {
        AnnotationKeyFactory.of(AnnotationKey.ARGS0.getCode(), "API")
    };

    private void verifyAnnotationKeys(List<AnnotationKey> annotationKeys, AnnotationKeyRegistryService annotationKeyRegistryService) {
        for (AnnotationKey key : annotationKeys) {
            assertSame(key, annotationKeyRegistryService.findAnnotationKey(key.getCode()));
        }
    }


    @Test
    public void testWithPlugins() {

        List<TraceMetadataProvider> typeProviders = Arrays.<TraceMetadataProvider>asList(new TestProvider(TEST_TYPES, TEST_KEYS));
        TraceMetadataLoaderService typeLoaderService = new DefaultTraceMetadataLoaderService(typeProviders);
        AnnotationKeyRegistryService annotationKeyRegistryService = new DefaultAnnotationKeyRegistryService(typeLoaderService);

        StaticFieldLookUp<AnnotationKey> lookUp = new StaticFieldLookUp<AnnotationKey>(AnnotationKey.class, AnnotationKey.class);
        verifyAnnotationKeys(lookUp.lookup(), annotationKeyRegistryService);


        verifyAnnotationKeys(Arrays.asList(TEST_KEYS), annotationKeyRegistryService);
    }
    
    @Test(expected=RuntimeException.class)
    public void testDuplicated() {

        List<TraceMetadataProvider> providers = Arrays.<TraceMetadataProvider>asList(
                new TestProvider(TEST_TYPES, TEST_KEYS),
                new TestProvider(new ServiceType[0], TEST_KEYS)
        );

        TraceMetadataLoader loader = new TraceMetadataLoader();
        loader.load(providers);
    }
    
    @Test(expected=RuntimeException.class)
    public void testDuplicated2() {
        List<TraceMetadataProvider> providers = Arrays.<TraceMetadataProvider>asList(
                new TestProvider(TEST_TYPES, TEST_KEYS),
                new TestProvider(TEST_TYPES, new AnnotationKey[0])
        );

        TraceMetadataLoader loader = new TraceMetadataLoader();
        loader.load(providers);
    }
    
    @Test(expected=RuntimeException.class)
    public void testDuplicated3() {
        List<TraceMetadataProvider> providers = Arrays.<TraceMetadataProvider>asList(
                new TestProvider(TEST_TYPES, TEST_KEYS),
                new TestProvider(TEST_TYPES, new AnnotationKey[0])
        );

        TraceMetadataLoader loader = new TraceMetadataLoader();
        loader.load(providers);
    }

    @Test(expected=RuntimeException.class)
    public void testDuplicatedWithDefault() {
        List<TraceMetadataProvider> providers = Arrays.<TraceMetadataProvider>asList(
                new TestProvider(DUPLICATED_CODE_WITH_DEFAULT_TYPE, TEST_KEYS)
        );

        TraceMetadataLoaderService loaderService = new DefaultTraceMetadataLoaderService(providers);
        ServiceTypeRegistryService serviceTypeRegistryService = new DefaultServiceTypeRegistryService(loaderService);
    }

    @Test(expected=RuntimeException.class)
    public void testDuplicatedWithDefault2() {
        List<TraceMetadataProvider> providers = Arrays.<TraceMetadataProvider>asList(
                new TestProvider(DUPLICATED_NAME_WITH_DEFAULT_TYPE, TEST_KEYS)
        );

        TraceMetadataLoaderService loaderService = new DefaultTraceMetadataLoaderService(providers);
        ServiceTypeRegistryService serviceTypeRegistryService = new DefaultServiceTypeRegistryService(loaderService);
    }

    @Test(expected=RuntimeException.class)
    public void testDuplicatedWithDefault3() {
        List<TraceMetadataProvider> providers = Arrays.<TraceMetadataProvider>asList(
                new TestProvider(TEST_TYPES, DUPLICATED_CODE_WITH_DEFAULT_KEY)
        );

        TraceMetadataLoaderService loaderService = new DefaultTraceMetadataLoaderService(providers);
        AnnotationKeyRegistryService annotationKeyRegistryService = new DefaultAnnotationKeyRegistryService(loaderService);

    }
    
    
    private static class TestProvider implements TraceMetadataProvider {
        private final ServiceType[] serviceTypes;
        private final AnnotationKey[] annotationKeys;
        
        public TestProvider(ServiceType[] serviceTypes, AnnotationKey[] annotationKeys) {
            this.serviceTypes = serviceTypes;
            this.annotationKeys = annotationKeys;
        }
        
        @Override
        public void setup(TraceMetadataSetupContext context) {
            for (ServiceType type : serviceTypes) {
                context.addServiceType(type);
            }

            for (AnnotationKey key : annotationKeys) {
                context.addAnnotationKey(key);
            }
        }
    }
}
