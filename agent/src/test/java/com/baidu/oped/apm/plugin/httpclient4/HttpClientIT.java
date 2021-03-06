/**
z * Copyright 2014 NAVER Corp.
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
package com.baidu.oped.apm.plugin.httpclient4;

import static com.baidu.oped.apm.bootstrap.plugin.test.Expectations.*;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.baidu.oped.apm.bootstrap.plugin.test.PluginTestVerifier;
import com.baidu.oped.apm.bootstrap.plugin.test.PluginTestVerifierHolder;
import com.baidu.oped.apm.test.plugin.Dependency;
import com.baidu.oped.apm.test.plugin.ApmPluginTestSuite;

/**
 * @author jaehong.kim
 */
@RunWith(ApmPluginTestSuite.class)
@Dependency({ "org.apache.httpcomponents:httpclient:[4.0],[4.0.1],[4.0.2],[4.0.3],[4.1],[4.1.1],[4.1.2],[4.1.3],[4.2],[4.2.1],[4.2.2],[4.2.3],[4.2.4],[4.2.4],[4.2.6]" })
public class HttpClientIT {
    @Test
    public void test() throws Exception {
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost("http://www.naver.com");
            post.addHeader("Content-Type", "application/json;charset=UTF-8");

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            httpClient.execute(post, responseHandler);
        } catch (Exception e) {
        } finally {
            if (null != httpClient && null != httpClient.getConnectionManager()) {
                httpClient.getConnectionManager().shutdown();
            }
        }

        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.printCache();

        Class<?> connectorClass;
        
        try {
            connectorClass = Class.forName("org.apache.http.impl.conn.ManagedClientConnectionImpl");
        } catch (ClassNotFoundException e) {
            connectorClass = Class.forName("org.apache.http.impl.conn.AbstractPooledConnAdapter");
        }
        
        verifier.verifyTrace(event("HTTP_CLIENT_4_INTERNAL", AbstractHttpClient.class.getMethod("execute", HttpUriRequest.class, ResponseHandler.class)));
        verifier.verifyTrace(event("HTTP_CLIENT_4_INTERNAL", connectorClass.getMethod("open", HttpRoute.class, HttpContext.class, HttpParams.class), annotation("http.internal.display", "www.naver.com")));
        verifier.verifyTrace(event("HTTP_CLIENT_4", HttpRequestExecutor.class.getMethod("execute", HttpRequest.class, HttpClientConnection.class, HttpContext.class), null, null, "www.naver.com", annotation("http.url", "/"), annotation("http.status.code", 200), annotation("http.io", anyAnnotationValue())));
        verifier.verifyTraceCount(0);
    }
}
