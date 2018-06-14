package org.apache.jasper.compiler;

import java.lang.reflect.Method;

import org.apache.jasper.JasperException;

public class MapperELVisitor extends ELNodeVisitor {
	private ValidateFunctionMapper fmapper;
	private ValidatorValidateVisitor validatorValidateVisitor;

	public MapperELVisitor(ValidateFunctionMapper fmapper,
			ValidatorValidateVisitor validatorValidateVisitor) {
		this.validatorValidateVisitor = validatorValidateVisitor;
		this.fmapper = fmapper;
	}

	@Override
	public void visit(ELNodeFunction n) throws JasperException {

		Class<?> c = null;
		Method method = null;
		try {
			c = validatorValidateVisitor.getLoader().loadClass(
					n.getFunctionInfo().getFunctionClass());
		} catch (ClassNotFoundException e) {
			validatorValidateVisitor.getErr().jspError(
					"jsp.error.function.classnotfound",
					n.getFunctionInfo().getFunctionClass(),
					n.getPrefix() + ':' + n.getName(), e.getMessage());
		}
		String paramTypes[] = n.getParameters();
		int size = paramTypes.length;
		Class<?> params[] = new Class[size];
		int i = 0;
		try {
			for (i = 0; i < size; i++) {
				params[i] = JspUtil.toClass(paramTypes[i],
						validatorValidateVisitor.getLoader());
			}
			method = c.getDeclaredMethod(n.getMethodName(), params);
		} catch (ClassNotFoundException e) {
			validatorValidateVisitor.getErr().jspError(
					"jsp.error.signature.classnotfound", paramTypes[i],
					n.getPrefix() + ':' + n.getName(), e.getMessage());
		} catch (NoSuchMethodException e) {
			validatorValidateVisitor.getErr().jspError(
					"jsp.error.noFunctionMethod", n.getMethodName(),
					n.getName(), c.getName());
		}
		fmapper.mapFunction(n.getPrefix() + ':' + n.getName(), method);
	}
}