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

package com.baidu.oped.apm.bootstrap;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.baidu.oped.apm.ProductInfo;

/**
 * @author emeroad
 * @author netspider
 */
public class ApmBootStrap {

    private static final Logger logger = Logger.getLogger(ApmBootStrap.class.getName());

    public static final String BOOT_CLASS = "com.baidu.oped.apm.profiler.DefaultAgent";

    private static final boolean STATE_NONE = false;
    private static final boolean STATE_STARTED = true;
    private static final AtomicBoolean LOAD_STATE = new AtomicBoolean(STATE_NONE);

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        if (agentArgs != null) {
            logger.info(ProductInfo.NAME + " agentArgs:" + agentArgs);
        }

        final boolean duplicated = checkDuplicateLoadState();
        if (duplicated) {
            logApmAgentLoadFail();
            return;
        }
        
        loadBootstrapCoreLib(instrumentation);

        ApmStarter bootStrap = new ApmStarter(agentArgs, instrumentation);
        bootStrap.start();

    }

    private static void loadBootstrapCoreLib(Instrumentation instrumentation) {
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

        JarFile bootStrapCoreJarFile = getBootStrapJarFile(bootStrapCoreJar);
        if (bootStrapCoreJarFile == null) {
            logger.severe("apm-bootstrap-core-x.x.x(-SNAPSHOT).jar not found");
            logApmAgentLoadFail();
            return;
        }
        logger.info("load apm-bootstrap-core-x.x.x(-SNAPSHOT).jar :" + bootStrapCoreJar);
        instrumentation.appendToBootstrapClassLoaderSearch(bootStrapCoreJarFile);
    }

    // for test
    static boolean getLoadState() {
        return LOAD_STATE.get();
    }

    private static boolean checkDuplicateLoadState() {
        final boolean startSuccess = LOAD_STATE.compareAndSet(STATE_NONE, STATE_STARTED);
        if (startSuccess) {
            return false;
        } else {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.severe("apm-bootstrap already started. skipping agent loading.");
            }
            return true;
        }
    }

    private static void logApmAgentLoadFail() {
        final String errorLog =
                "*****************************************************************************\n" +
                        "* Apm Agent load failure\n" +
                        "*****************************************************************************";
        System.err.println(errorLog);
    }


    private static JarFile getBootStrapJarFile(String bootStrapCoreJar) {
        try {
            return new JarFile(bootStrapCoreJar);
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, bootStrapCoreJar + " file not found.", ioe);
            return null;
        }
    }

}
