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
package com.baidu.oped.apm.profiler.plugin;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.oped.apm.bootstrap.plugin.ProfilerPlugin;
import com.baidu.oped.apm.common.plugin.PluginLoader;
import com.baidu.oped.apm.profiler.DefaultAgent;
import com.baidu.oped.apm.profiler.instrument.ClassInjector;
import com.baidu.oped.apm.profiler.instrument.JarProfilerPluginClassInjector;

/**
 * @author Jongho Moon
 *
 */
public class ProfilerPluginLoader {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DefaultAgent agent;
    
    public ProfilerPluginLoader(DefaultAgent agent) {
        this.agent = agent;
    }
    
    public List<DefaultProfilerPluginContext> load(URL[] pluginJars) {
        List<DefaultProfilerPluginContext> pluginContexts = new ArrayList<DefaultProfilerPluginContext>(pluginJars.length);
        List<String> disabled = agent.getProfilerConfig().getDisabledPlugins();
        
        for (URL jar : pluginJars) {
            List<ProfilerPlugin> plugins = PluginLoader.load(ProfilerPlugin.class, new URL[] { jar });
            
            for (ProfilerPlugin plugin : plugins) {
                if (disabled.contains(plugin.getClass().getName())) {
                    logger.info("Skip disabled plugin: {}", plugin.getClass().getName());
                    continue;
                }
                
                logger.info("Loading plugin: {}", plugin.getClass().getName());

                final DefaultProfilerPluginContext context = setupPlugin(jar, plugin);
                pluginContexts.add(context);
            }
        }
        
        
        return pluginContexts;
    }

    private DefaultProfilerPluginContext setupPlugin(URL jar, ProfilerPlugin plugin) {
        final ClassInjector classInjector = JarProfilerPluginClassInjector.of(agent.getInstrumentation(), agent.getClassPool(), jar);
        final DefaultProfilerPluginContext context = new DefaultProfilerPluginContext(agent, classInjector);
        final GuardProfilerPluginContext guard = new GuardProfilerPluginContext(context);
        try {
            // WARN external plugin api
            plugin.setup(guard);
        } finally {
            guard.close();
        }
        return context;
    }

}
