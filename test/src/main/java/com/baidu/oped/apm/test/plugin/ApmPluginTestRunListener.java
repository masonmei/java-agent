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

import java.io.OutputStream;
import java.io.PrintWriter;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * @author Jongho Moon
 *
 */
public class ApmPluginTestRunListener extends RunListener implements ApmPluginTestConstants {
    private final PrintWriter out;
    
    public ApmPluginTestRunListener(OutputStream out) {
        this(new PrintWriter(out));
    }
    
    public ApmPluginTestRunListener(PrintWriter out) {
        this.out = out;
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        out.println(JUNIT_OUTPUT_DELIMITER + "testRunStarted");
        out.flush();
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        out.println(JUNIT_OUTPUT_DELIMITER + "testRunFinished");
        out.flush();
    }

    @Override
    public void testStarted(Description description) throws Exception {
        out.println(JUNIT_OUTPUT_DELIMITER + "testStarted" + JUNIT_OUTPUT_DELIMITER + description.getDisplayName());
        out.flush();
    }

    @Override
    public void testFinished(Description description) throws Exception {
        out.println(JUNIT_OUTPUT_DELIMITER + "testFinished" + JUNIT_OUTPUT_DELIMITER + description.getDisplayName());
        out.flush();
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        out.println(JUNIT_OUTPUT_DELIMITER + "testFailure" + JUNIT_OUTPUT_DELIMITER + failureToString(failure));
        out.flush();
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        out.println(JUNIT_OUTPUT_DELIMITER + "testAssumptionFailure" + JUNIT_OUTPUT_DELIMITER + failureToString(failure));
        out.flush();
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        out.println(JUNIT_OUTPUT_DELIMITER + "testIgnored" + JUNIT_OUTPUT_DELIMITER + description.getDisplayName());
        out.flush();
    }

    private String failureToString(Failure failure) {
        StringBuilder builder = new StringBuilder();

        builder.append(failure.getTestHeader());
        builder.append(JUNIT_OUTPUT_DELIMITER);
        builder.append(failure.getException().getClass().getName());
        builder.append(JUNIT_OUTPUT_DELIMITER);
        builder.append(failure.getMessage());
        builder.append(JUNIT_OUTPUT_DELIMITER);

        for (StackTraceElement e : failure.getException().getStackTrace()) {
            builder.append(e.getClassName());
            builder.append(',');
            builder.append(e.getMethodName());
            builder.append(',');
            builder.append(e.getFileName());
            builder.append(',');
            builder.append(e.getLineNumber());

            builder.append(JUNIT_OUTPUT_DELIMITER);
        }

        return builder.toString();
    }
}
