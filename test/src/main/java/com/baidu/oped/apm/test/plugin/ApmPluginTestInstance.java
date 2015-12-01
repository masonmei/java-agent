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
package com.baidu.oped.apm.test.plugin;

import java.io.File;
import java.util.List;
import java.util.Scanner;

/**
 * @author Jongho Moon
 *
 */
public interface ApmPluginTestInstance {
    public String getTestId();
    public List<String> getClassPath();
    public List<String> getVmArgs();
    public String getMainClass();
    public List<String> getAppArgs();
    public File getWorkingDirectory();
    
    public Scanner startTest(Process process) throws Throwable;
    public void endTest(Process process) throws Throwable;
}
