/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package javax.servlet.jsp.el;

import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.PageContext;

/**
 *
 * @since 2.1
 */
public class ImplicitObjectELResolver extends ELResolver {

    private static final String[] SCOPE_NAMES = new String[] {
            "applicationScope", "cookie", "header", "headerValues",
            "initParam", "pageContext", "pageScope", "param", "paramValues",
            "requestScope", "sessionScope" };

    private static final int APPLICATIONSCOPE = 0;

    private static final int COOKIE = 1;

    private static final int HEADER = 2;

    private static final int HEADERVALUES = 3;

    private static final int INITPARAM = 4;

    private static final int PAGECONTEXT = 5;

    private static final int PAGESCOPE = 6;

    private static final int PARAM = 7;

    private static final int PARAM_VALUES = 8;

    private static final int REQUEST_SCOPE = 9;

    private static final int SESSION_SCOPE = 10;

    public ImplicitObjectELResolver() {
        super();
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property)
            throws NullPointerException, PropertyNotFoundException, ELException {
        if (context == null) {
            throw new NullPointerException();
        }

        if (base == null && property != null) {
            int idx = Arrays.binarySearch(SCOPE_NAMES, property.toString());

            if (idx >= 0) {
                PageContext page = (PageContext) context
                        .getContext(JspContext.class);
                context.setPropertyResolved(true);
                switch (idx) {
                case APPLICATIONSCOPE:
                    return ImplicitObjectELResolverScopeManager.get(page).getApplicationScope();
                case COOKIE:
                    return ImplicitObjectELResolverScopeManager.get(page).getCookie();
                case HEADER:
                    return ImplicitObjectELResolverScopeManager.get(page).getHeader();
                case HEADERVALUES:
                    return ImplicitObjectELResolverScopeManager.get(page).getHeaderValues();
                case INITPARAM:
                    return ImplicitObjectELResolverScopeManager.get(page).getInitParam();
                case PAGECONTEXT:
                    return ImplicitObjectELResolverScopeManager.get(page).getPageContext();
                case PAGESCOPE:
                    return ImplicitObjectELResolverScopeManager.get(page).getPageScope();
                case PARAM:
                    return ImplicitObjectELResolverScopeManager.get(page).getParam();
                case PARAM_VALUES:
                    return ImplicitObjectELResolverScopeManager.get(page).getParamValues();
                case REQUEST_SCOPE:
                    return ImplicitObjectELResolverScopeManager.get(page).getRequestScope();
                case SESSION_SCOPE:
                    return ImplicitObjectELResolverScopeManager.get(page).getSessionScope();
                }
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" }) // TCK signature test fails with generics
    public Class getType(ELContext context, Object base, Object property)
            throws NullPointerException, PropertyNotFoundException, ELException {
        if (context == null) {
            throw new NullPointerException();
        }

        if (base == null && property != null) {
            int idx = Arrays.binarySearch(SCOPE_NAMES, property.toString());
            if (idx >= 0) {
                context.setPropertyResolved(true);
            }
        }
        return null;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property,
            Object value) throws NullPointerException,
            PropertyNotFoundException, PropertyNotWritableException,
            ELException {
        if (context == null) {
            throw new NullPointerException();
        }

        if (base == null && property != null) {
            int idx = Arrays.binarySearch(SCOPE_NAMES, property.toString());
            if (idx >= 0) {
                context.setPropertyResolved(true);
                throw new PropertyNotWritableException();
            }
        }
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property)
            throws NullPointerException, PropertyNotFoundException, ELException {
        if (context == null) {
            throw new NullPointerException();
        }

        if (base == null && property != null) {
            int idx = Arrays.binarySearch(SCOPE_NAMES, property.toString());
            if (idx >= 0) {
                context.setPropertyResolved(true);
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        List<FeatureDescriptor> feats = new ArrayList<FeatureDescriptor>(
                SCOPE_NAMES.length);
        FeatureDescriptor feat;
        for (int i = 0; i < SCOPE_NAMES.length; i++) {
            feat = new FeatureDescriptor();
            feat.setDisplayName(SCOPE_NAMES[i]);
            feat.setExpert(false);
            feat.setHidden(false);
            feat.setName(SCOPE_NAMES[i]);
            feat.setPreferred(true);
            feat.setValue(getResolvableAtDesignTime(), Boolean.TRUE);
            feat.setValue(getType(), String.class);
            feats.add(feat);
        }
        return feats.iterator();
    }

    @Override
    public Class<String> getCommonPropertyType(ELContext context, Object base) {
        if (base == null) {
            return String.class;
        }
        return null;
    }
}
