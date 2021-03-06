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
package com.baidu.oped.apm.bootstrap;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.baidu.oped.apm.ProductInfo;
import com.baidu.oped.apm.bootstrap.config.DefaultProfilerConfig;
import com.baidu.oped.apm.bootstrap.config.ProfilerConfig;
import com.baidu.oped.apm.bootstrap.util.IdValidateUtils;
import com.baidu.oped.apm.common.ApmConstants;
import com.baidu.oped.apm.common.Version;
import com.baidu.oped.apm.common.service.AnnotationKeyRegistryService;
import com.baidu.oped.apm.common.service.DefaultAnnotationKeyRegistryService;
import com.baidu.oped.apm.common.service.DefaultServiceTypeRegistryService;
import com.baidu.oped.apm.common.service.DefaultTraceMetadataLoaderService;
import com.baidu.oped.apm.common.service.ServiceTypeRegistryService;
import com.baidu.oped.apm.common.service.TraceMetadataLoaderService;
import com.baidu.oped.apm.common.util.BytesUtils;
import com.baidu.oped.apm.common.util.ApmThreadFactory;
import com.baidu.oped.apm.common.util.SimpleProperty;
import com.baidu.oped.apm.common.util.SystemProperty;
import com.baidu.oped.apm.exception.ApmException;

/**
 * @author Jongho Moon
 *
 */
public class ApmStarter {
    private static final Logger logger = Logger.getLogger(ApmStarter   .class.getName());

    public static final String BOOT_CLASS = "com.baidu.oped.apm.profiler.DefaultAgent";

    private SimpleProperty systemProperty = SystemProperty.INSTANCE;
    private final String agentArgs;
    private final Map<String, String> argMap;
    private final Instrumentation instrumentation;


    public ApmStarter(String agentArgs, Instrumentation instrumentation) {
        if (agentArgs != null) {
            logger.info(ProductInfo.NAME + " agentArgs:" + agentArgs);
        }
        if (instrumentation == null) {
            throw new NullPointerException("instrumentation must not be null");
        }

        this.agentArgs = agentArgs;
        this.argMap = parseAgentArgs(agentArgs);
        this.instrumentation = instrumentation;
    }

    public void start() {
        // 1st find boot-strap.jar
        final ClassPathResolver classPathResolver = new ClassPathResolver();
        boolean agentJarNotFound = classPathResolver.findAgentJar();
        if (!agentJarNotFound) {
            logger.severe("apm-bootstrap-x.x.x(-SNAPSHOT).jar Fnot found.");
            logApmAgentLoadFail();
            return;
        }
        
        // 2nd find boot-strap-core.jar
        final String bootStrapCoreJar = classPathResolver.getBootStrapCoreJar();
        if (bootStrapCoreJar == null) {
            logger.severe("apm-bootstrap-core-x.x.x(-SNAPSHOT).jar not found");
            logApmAgentLoadFail();
            return;
        }

        
        if (!isValidId("apm.agentId", ApmConstants.AGENT_NAME_MAX_LEN)) {
            logApmAgentLoadFail();
            return;
        }
        if (!isValidId("apm.applicationName", ApmConstants.APPLICATION_NAME_MAX_LEN)) {
            logApmAgentLoadFail();
            return;
        }

        URL[] pluginJars = classPathResolver.resolvePlugins();
        TraceMetadataLoaderService typeLoaderService = new DefaultTraceMetadataLoaderService(pluginJars);
        ServiceTypeRegistryService serviceTypeRegistryService  = new DefaultServiceTypeRegistryService(typeLoaderService);
        AnnotationKeyRegistryService annotationKeyRegistryService = new DefaultAnnotationKeyRegistryService(typeLoaderService);

        String configPath = getConfigPath(classPathResolver);
        if (configPath == null) {
            logApmAgentLoadFail();
            return;
        }

        // set the path of log file as a system property
        saveLogFilePath(classPathResolver);

        saveApmVersion();

        try {
            // Is it right to load the configuration in the bootstrap?
            ProfilerConfig profilerConfig = DefaultProfilerConfig.load(configPath);

            // this is the library list that must be loaded
            List<URL> libUrlList = resolveLib(classPathResolver);
            AgentClassLoader agentClassLoader = new AgentClassLoader(libUrlList.toArray(new URL[libUrlList.size()]));
            String bootClass = argMap.containsKey("bootClass") ? argMap.get("bootClass") : BOOT_CLASS;
            agentClassLoader.setBootClass(bootClass);
            logger.info("apm agent [" + bootClass + "] starting...");

            AgentOption option = createAgentOption(agentArgs, instrumentation, profilerConfig, pluginJars, null, serviceTypeRegistryService, annotationKeyRegistryService);
            Agent apmAgent = agentClassLoader.boot(option);
            apmAgent.start();
            registerShutdownHook(apmAgent);
            logger.info("apm agent started normally.");
        } catch (Exception e) {
            // unexpected exception that did not be checked above
            logger.log(Level.SEVERE, ProductInfo.NAME + " start failed. Error:" + e.getMessage(), e);
            logApmAgentLoadFail();
        }
    }

    private AgentOption createAgentOption(String agentArgs, Instrumentation instrumentation, ProfilerConfig profilerConfig, URL[] pluginJars, String bootStrapJarPath, ServiceTypeRegistryService serviceTypeRegistryService, AnnotationKeyRegistryService annotationKeyRegistryService) {

        return new DefaultAgentOption(agentArgs, instrumentation, profilerConfig, pluginJars, bootStrapJarPath, serviceTypeRegistryService, annotationKeyRegistryService);
    }

    // for test
    void setSystemProperty(SimpleProperty systemProperty) {
        this.systemProperty = systemProperty;
    }

    private void registerShutdownHook(final Agent apmAgent) {
        final Runnable stop = new Runnable() {
            @Override
            public void run() {
                apmAgent.stop();
            }
        };
        ApmThreadFactory apmThreadFactory = new ApmThreadFactory("Apm-shutdown-hook");
        Thread thread = apmThreadFactory.newThread(stop);
        Runtime.getRuntime().addShutdownHook(thread);
    }

    private Map<String, String> parseAgentArgs(String str) {
        Map<String, String> map = new HashMap<String, String>();

        if (str == null || str.isEmpty()) {
            return map;
        }

        Scanner scanner = new Scanner(str);
        scanner.useDelimiter("\\s*,\\s*");

        while (scanner.hasNext()) {
            String token = scanner.next();
            int assign = token.indexOf('=');

            if (assign == -1) {
                map.put(token, "");
            } else {
                map.put(token.substring(0, assign), token.substring(assign + 1));
            }
        }
        scanner.close();
        return Collections.unmodifiableMap(map);
    }

    private void logApmAgentLoadFail() throws ApmException {
        final String errorLog =
                "*****************************************************************************\n" +
                        "* Apm Agent load failure\n" +
                        "*****************************************************************************";
        System.err.println(errorLog);
    }


    private boolean isValidId(String propertyName, int maxSize) {
        logger.info("check -D" + propertyName);
        String value = systemProperty.getProperty(propertyName);
        if (value == null){
            logger.severe("-D" + propertyName + " is null. value:null");
            return false;
        }
        // blanks not permitted around value
        value = value.trim();
        if (value.isEmpty()) {
            logger.severe("-D" + propertyName + " is empty. value:''");
            return false;
        }

        if (!IdValidateUtils.validateId(value, maxSize)) {
            logger.severe("invalid Id. " + propertyName + " can only contain [a-zA-Z0-9], '.', '-', '_'. maxLength:" + maxSize + " value:" + value);
            return false;
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.info("check success. -D" + propertyName + ":" + value + " length:" + getLength(value));
        }
        return true;
    }

    private int getLength(String value) {
        final byte[] bytes = BytesUtils.toBytes(value);
        if (bytes == null) {
            return 0;
        } else {
            return bytes.length;
        }
    }


    private void saveLogFilePath(ClassPathResolver classPathResolver) {
        String agentLogFilePath = classPathResolver.getAgentLogFilePath();
        logger.info("logPath:" + agentLogFilePath);

        systemProperty.setProperty(ProductInfo.NAME + ".log", agentLogFilePath);
    }

    private void saveApmVersion() {
        logger.info("apm version:" + Version.VERSION);
        systemProperty.setProperty(ProductInfo.NAME + ".version", Version.VERSION);
    }

    private String getConfigPath(ClassPathResolver classPathResolver) {
        final String configName = ProductInfo.NAME + ".config";
        String apmConfigFormSystemProperty = systemProperty.getProperty(configName);
        if (apmConfigFormSystemProperty != null) {
            logger.info(configName + " systemProperty found. " + apmConfigFormSystemProperty);
            return apmConfigFormSystemProperty;
        }

        String classPathAgentConfigPath = classPathResolver.getAgentConfigPath();
        if (classPathAgentConfigPath != null) {
            logger.info("classpath " + configName +  " found. " + classPathAgentConfigPath);
            return classPathAgentConfigPath;
        }

        logger.severe(configName + " file not found.");
        return null;
    }


    private List<URL> resolveLib(ClassPathResolver classPathResolver)  {
        // this method may handle only absolute path,  need to handle relative path (./..agentlib/lib)
        String agentJarFullPath = classPathResolver.getAgentJarFullPath();
        String agentLibPath = classPathResolver.getAgentLibPath();
        List<URL> urlList = classPathResolver.resolveLib();
        String agentConfigPath = classPathResolver.getAgentConfigPath();

        if (logger.isLoggable(Level.INFO)) {
            logger.info("agentJarPath:" + agentJarFullPath);
            logger.info("agentLibPath:" + agentLibPath);
            logger.info("agent lib list:" + urlList);
            logger.info("agent config:" + agentConfigPath);
        }

        return urlList;
    }

}
