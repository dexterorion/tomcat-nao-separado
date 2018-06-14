package org.apache.jasper.compiler;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Hashtable;

import org.apache.jasper.JasperException;

/**
 * Class storing the result of introspecting a custom tag handler.
 */
public class GeneratorTagHandlerInfo {

    private Hashtable<String, Method> methodMaps;

    private Hashtable<String, Class<?>> propertyEditorMaps;

    private Class<?> tagHandlerClass;

    /**
     * Constructor.
     *
     * @param n
     *            The custom tag whose tag handler class is to be
     *            introspected
     * @param tagHandlerClass
     *            Tag handler class
     * @param err
     *            Error dispatcher
     */
    public GeneratorTagHandlerInfo(Node n, Class<?> tagHandlerClass,
            ErrorDispatcher err) throws JasperException {
        this.tagHandlerClass = tagHandlerClass;
        this.methodMaps = new Hashtable<String, Method>();
        this.propertyEditorMaps = new Hashtable<String, Class<?>>();

        try {
            BeanInfo tagClassInfo = Introspector
                    .getBeanInfo(tagHandlerClass);
            PropertyDescriptor[] pd = tagClassInfo.getPropertyDescriptors();
            for (int i = 0; i < pd.length; i++) {
                /*
                 * FIXME: should probably be checking for things like
                 * pageContext, bodyContent, and parent here -akv
                 */
                if (pd[i].getWriteMethod() != null) {
                    methodMaps.put(pd[i].getName(), pd[i].getWriteMethod());
                }
                if (pd[i].getPropertyEditorClass() != null)
                    propertyEditorMaps.put(pd[i].getName(), pd[i]
                            .getPropertyEditorClass());
            }
        } catch (IntrospectionException ie) {
            err.jspError(n, ie, "jsp.error.introspect.taghandler",
                    tagHandlerClass.getName());
        }
    }

    /**
     * XXX
     */
    public Method getSetterMethod(String attrName) {
        return methodMaps.get(attrName);
    }

    /**
     * XXX
     */
    public Class<?> getPropertyEditorClass(String attrName) {
        return propertyEditorMaps.get(attrName);
    }

    /**
     * XXX
     */
    public Class<?> getTagHandlerClass() {
        return tagHandlerClass;
    }
}