package javax.el;

import java.lang.reflect.Constructor;

public class UtilConstructorWrapper extends UtilWrapper {
    private final Constructor<?> c;

    public UtilConstructorWrapper(Constructor<?> c) {
        this.c = c;
    }

    @Override
    public Object unWrap() {
        return c;
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return c.getParameterTypes();
    }

    @Override
    public boolean isVarArgs() {
        return c.isVarArgs();
    }

    @Override
    public boolean isBridge() {
        return false;
    }
}