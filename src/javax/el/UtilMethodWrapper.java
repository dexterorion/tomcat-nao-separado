package javax.el;

import java.lang.reflect.Method;

public class UtilMethodWrapper extends UtilWrapper {
    private final Method m;

    public UtilMethodWrapper(Method m) {
        this.m = m;
    }

    @Override
    public Object unWrap() {
        return m;
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return m.getParameterTypes();
    }

    @Override
    public boolean isVarArgs() {
        return m.isVarArgs();
    }

    @Override
    public boolean isBridge() {
        return m.isBridge();
    }
}