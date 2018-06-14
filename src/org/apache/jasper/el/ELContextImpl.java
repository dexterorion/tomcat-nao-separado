/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jasper.el;

import java.lang.reflect.Method;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper2;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ResourceBundleELResolver;
import javax.el.VariableMapper;

import org.apache.jasper.Constants28;

/**
 * Implementation of ELContext
 * 
 * @author Jacob Hookom
 */
public final class ELContextImpl extends ELContext {

    private static final FunctionMapper2 NullFunctionMapper = new FunctionMapper2() {
        @Override
        public Method resolveFunction(String prefix, String localName) {
            return null;
        }
    };

    private static final ELResolver DefaultResolver;

    static {
        if (Constants28.isSecurityEnabled()) {
            DefaultResolver = null;
        } else {
            DefaultResolver = new CompositeELResolver();
            ((CompositeELResolver) DefaultResolver).add(new MapELResolver());
            ((CompositeELResolver) DefaultResolver).add(new ResourceBundleELResolver());
            ((CompositeELResolver) DefaultResolver).add(new ListELResolver());
            ((CompositeELResolver) DefaultResolver).add(new ArrayELResolver());
            ((CompositeELResolver) DefaultResolver).add(new BeanELResolver());
        }
    }

    private final ELResolver resolver;

    private FunctionMapper2 functionMapper = NullFunctionMapper;

    private VariableMapper variableMapper;

    public ELContextImpl() {
        this(getDefaultResolver());
    }

    public ELContextImpl(ELResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public ELResolver getELResolver() {
        return this.resolver;
    }

    @Override
    public FunctionMapper2 getFunctionMapper() {
        return this.functionMapper;
    }

    @Override
    public VariableMapper getVariableMapper() {
        if (this.variableMapper == null) {
            this.variableMapper = new ELContextImplVariableMapperImpl();
        }
        return this.variableMapper;
    }

    public void setFunctionMapper(FunctionMapper2 functionMapper) {
        this.functionMapper = functionMapper;
    }

    public void setVariableMapper(VariableMapper variableMapper) {
        this.variableMapper = variableMapper;
    }

    public static ELResolver getDefaultResolver() {
        if (Constants28.isSecurityEnabled()) {
            CompositeELResolver defaultResolver = new CompositeELResolver();
            defaultResolver.add(new MapELResolver());
            defaultResolver.add(new ResourceBundleELResolver());
            defaultResolver.add(new ListELResolver());
            defaultResolver.add(new ArrayELResolver());
            defaultResolver.add(new BeanELResolver());
            return defaultResolver;
        } else {
            return DefaultResolver;
        }
    }
}
