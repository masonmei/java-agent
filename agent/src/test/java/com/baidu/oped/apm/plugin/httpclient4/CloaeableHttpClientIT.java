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

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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
@Dependency({ "org.apache.httpcomponents:httpclient:[4.3],[4.3.1],[4.3.2],[4.3.3],[4.3.4],[4.4],[4.4.1],[4.5]" })
public class CloaeableHttpClientIT {
    @Test
    public void test() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpGet httpget = new HttpGet("http://www.naver.com");
            CloseableHttpResponse response = httpclient.execute(httpget);
            try {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream instream = entity.getContent();
                    try {
                        instream.read();
                    } catch (IOException ex) {
                        throw ex;
                    } finally {
                        instream.close();
                    }
                }
            } finally {
                response.close();
            }
        } finally {
            httpclient.close();
        }

        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.printCache();
        
        verifier.verifyTrace(event("HTTP_CLIENT_4_INTERNAL", CloseableHttpClient.class.getMethod("execute", HttpUriRequest.class)));
        verifier.verifyTrace(event("HTTP_CLIENT_4_INTERNAL", PoolingHttpClientConnectionManager.class.getMethod("connect", HttpClientConnection.class, HttpRoute.class, int.class, HttpContext.class), annotation("http.internal.display", "www.naver.com:80")));
        verifier.verifyTrace(event("HTTP_CLIENT_4", HttpRequestExecutor.class.getMethod("execute", HttpRequest.class, HttpClientConnection.class, HttpContext.class), null, null, "www.naver.com", annotation("http.url", "/"), annotation("http.status.code", 200), annotation("http.io", anyAnnotationValue())));
        verifier.verifyTraceCount(0);
    }
}
