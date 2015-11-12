package com.baidu.oped.apm.profiler.plugin.xml;

import com.baidu.oped.apm.bootstrap.instrument.InstrumentClass;
import com.baidu.oped.apm.bootstrap.instrument.InstrumentException;
import com.baidu.oped.apm.profiler.plugin.xml.transformer.ClassRecipe;

public class OverrideMethodInjector implements ClassRecipe {
    private final String methodName;
    private final String[] paramTypes;
    
    public OverrideMethodInjector(String methodName, String[] paramTypes) {
        this.methodName = methodName;
        this.paramTypes = paramTypes;
    }

    @Override
    public void edit(ClassLoader classLoader, InstrumentClass target) throws InstrumentException {
        target.addDelegatorMethod(methodName, paramTypes);
    }

    @Override
    public String toString() {
        return "OverrideMethodInjector[methodName=" + methodName + ", paramTypes" + paramTypes + "]";
    }
}
