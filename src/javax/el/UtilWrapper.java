package javax.el;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class UtilWrapper {

    public static List<UtilWrapper> wrap(Method[] methods, String name) {
        List<UtilWrapper> result = new ArrayList<UtilWrapper>();
        for (Method method : methods) {
            if (method.getName().equals(name)) {
                result.add(new UtilMethodWrapper(method));
            }
        }
        return result;
    }

    public static List<UtilWrapper> wrap(Constructor<?>[] constructors) {
        List<UtilWrapper> result = new ArrayList<UtilWrapper>();
        for (Constructor<?> constructor : constructors) {
            result.add(new UtilConstructorWrapper(constructor));
        }
        return result;
    }

    public abstract Object unWrap();
    public abstract Class<?>[] getParameterTypes();
    public abstract boolean isVarArgs();
    public abstract boolean isBridge();
}