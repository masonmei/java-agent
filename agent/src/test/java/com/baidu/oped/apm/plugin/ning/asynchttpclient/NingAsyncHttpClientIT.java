package com.baidu.oped.apm.plugin.ning.asynchttpclient;

import static com.baidu.oped.apm.bootstrap.plugin.test.Expectations.*;

import java.util.concurrent.Future;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.baidu.oped.apm.bootstrap.plugin.test.PluginTestVerifier;
import com.baidu.oped.apm.bootstrap.plugin.test.PluginTestVerifierHolder;
import com.baidu.oped.apm.test.plugin.Dependency;
import com.baidu.oped.apm.test.plugin.JvmVersion;
import com.baidu.oped.apm.test.plugin.ApmPluginTestSuite;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

/**
 * @author netspider
 */
@RunWith(ApmPluginTestSuite.class)
@Dependency({ "com.ning:async-http-client:[1.7.24],[1.8.16,1.8.999)" })
@JvmVersion(7)
public class NingAsyncHttpClientIT {
    
    @Test
    public void test() throws Exception {
        AsyncHttpClient client = new AsyncHttpClient();
        
        try {
            Future<Response> f = client.preparePost("http://www.naver.com/").addParameter("param1", "value1").execute();
            Response response = f.get();
        } finally {
            client.close();
        }
        
        PluginTestVerifier verifier = PluginTestVerifierHolder.getInstance();
        verifier.printCache();
        
        verifier.verifyTrace(event("ASYNC_HTTP_CLIENT", AsyncHttpClient.class.getMethod("executeRequest", Request.class, AsyncHandler.class), null, null, "www.naver.com", annotation("http.url", "http://www.naver.com"), annotation("http.param", "param1=value1")));
        verifier.verifyTraceCount(0);
   }
}