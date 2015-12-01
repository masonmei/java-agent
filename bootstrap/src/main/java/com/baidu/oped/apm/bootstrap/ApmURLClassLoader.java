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

import java.net.URL;
import java.net.URLClassLoader;

/**
 * ApmURLClassLoader loads a class in the profiler lib directory and delegates to load the other classes to parent classloader
 * Dead lock could happen in case of standalone java application.
 * Don't delegate to parents classlaoder if classes are in the profiler lib directory
 *
 * @author emeroad
 */
public class ApmURLClassLoader extends URLClassLoader {

    private static final LibClass PROFILER_LIB_CLASS = new ProfilerLibClass();

    private final ClassLoader parent;

    private final LibClass libClass;

    public ApmURLClassLoader(URL[] urls, ClassLoader parent, LibClass libClass) {
        super(urls, parent);
        if (parent == null) {
            throw new NullPointerException("parent must not be null");
        }
        if (libClass == null) {
            throw new NullPointerException("libClass must not be null");
        }
        this.parent = parent;
        this.libClass = libClass;
    }

    public ApmURLClassLoader(URL[] urls, ClassLoader parent) {
        this(urls, parent, PROFILER_LIB_CLASS);
    }


    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // First, check if the class has already been loaded
        Class clazz = findLoadedClass(name);
        if (clazz == null) {
            if (onLoadClass(name)) {
                // load a class used for Apm itself by this ApmURLClassLoader
                clazz = findClass(name);
            } else {
                try {
                    // load a class by parent ClassLoader
                    clazz = parent.loadClass(name);
                } catch (ClassNotFoundException ignore) {
                }
                if (clazz == null) {
                    // if not found, try to load a class by this ApmURLClassLoader
                    clazz = findClass(name);
                }
            }
        }
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

    // for test
    boolean onLoadClass(String name) {
        return libClass.onLoadClass(name);
    }

}