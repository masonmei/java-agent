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

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * @author Jongho Moon
 *
 */
public class ApmPluginTestRunner extends BlockJUnit4ClassRunner {
    private final ApmPluginTestContext context;
    private final ApmPluginTestInstance testCase;

    ApmPluginTestRunner(ApmPluginTestContext context, ApmPluginTestInstance testCase) throws InitializationError {
        super(context.getTestClass());
        
        this.context = context;
        this.testCase = testCase;
    }

    @Override
    protected String getName() {
        return String.format("[%s]", testCase.getTestId());
    }

    @Override
    protected String testName(final FrameworkMethod method) {
        return String.format("%s[%s]", method.getName(), testCase.getTestId());
    }

    @Override
    protected Statement classBlock(RunNotifier notifier) {
        return new ApmPluginTestStatement(this, notifier, context, testCase);
    }
}
