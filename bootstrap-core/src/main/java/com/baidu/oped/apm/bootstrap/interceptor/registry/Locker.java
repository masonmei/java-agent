package com.baidu.oped.apm.bootstrap.interceptor.registry;


/**
 * @author emeroad
 */
public interface Locker {

    boolean lock(Object lock);

    boolean unlock(Object lock);
}
