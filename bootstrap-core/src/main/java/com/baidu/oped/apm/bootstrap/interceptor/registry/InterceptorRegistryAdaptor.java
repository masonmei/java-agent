package com.baidu.oped.apm.bootstrap.interceptor.registry;

import com.baidu.oped.apm.bootstrap.interceptor.Interceptor;


/**
 * @author emeroad
 */
public interface InterceptorRegistryAdaptor {
    int addInterceptor(Interceptor interceptor);
    Interceptor getInterceptor(int key);
}
