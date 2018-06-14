package org.apache.jasper.compiler;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.jsp.tagext.FunctionInfo;

import org.apache.jasper.Constants28;
import org.apache.jasper.JasperException;
import org.apache.tomcat.util.security.PrivilegedGetTccl;

/**
 * A visitor for the page.  The places where EL is allowed are scanned
 * for functions, and if found functions mappers are created.
 */
public class ElFunctionMapperELFunctionVisitor extends NodeVisitor {
    
    /**
	 * 
	 */
	private final ELFunctionMapper elFunctionMapper;

	/**
	 * @param elFunctionMapper
	 */
	public ElFunctionMapperELFunctionVisitor(ELFunctionMapper elFunctionMapper) {
		this.elFunctionMapper = elFunctionMapper;
	}

	/**
     * Use a global name map to facilitate reuse of function maps.
     * The key used is prefix:function:uri.
     */
    private final HashMap<String, String> gMap = new HashMap<String, String>();

    @Override
    public void visit(NodeParamAction n) throws JasperException {
        doMap(n.getValue());
        visitBody(n);
    }

    @Override
    public void visit(NodeIncludeAction n) throws JasperException {
        doMap(n.getPage());
        visitBody(n);
    }

    @Override
    public void visit(NodeForwardAction n) throws JasperException {
        doMap(n.getPage());
        visitBody(n);
    }

    @Override
    public void visit(NodeSetProperty n) throws JasperException {
        doMap(n.getValue());
        visitBody(n);
    }

    @Override
    public void visit(NodeUseBean n) throws JasperException {
        doMap(n.getBeanName());
        visitBody(n);
    }

    @Override
    public void visit(NodePlugIn n) throws JasperException {
        doMap(n.getHeight());
        doMap(n.getWidth());
        visitBody(n);
    }

    @Override
    public void visit(NodeJspElement n) throws JasperException {

        NodeJspAttribute[] attrs = n.getJspAttributes();
        for (int i = 0; attrs != null && i < attrs.length; i++) {
            doMap(attrs[i]);
        }
        doMap(n.getNameAttribute());
        visitBody(n);
    }

    @Override
    public void visit(NodeUninterpretedTag n) throws JasperException {

        NodeJspAttribute[] attrs = n.getJspAttributes();
        for (int i = 0; attrs != null && i < attrs.length; i++) {
            doMap(attrs[i]);
        }
        visitBody(n);
    }

    @Override
    public void visit(NodeCustomTag n) throws JasperException {
        NodeJspAttribute[] attrs = n.getJspAttributes();
        for (int i = 0; attrs != null && i < attrs.length; i++) {
            doMap(attrs[i]);
        }
        visitBody(n);
    }

    @Override
    public void visit(NodeELExpression n) throws JasperException {
        doMap(n.getEL());
    }

    private void doMap(NodeJspAttribute attr) 
            throws JasperException {
        if (attr != null) {
            doMap(attr.getEL());
        }
    }

    /**
     * Creates function mappers, if needed, from ELNodes
     */
    private void doMap(ELNodeNodes el) 
            throws JasperException {

        if (el == null) {
            return;
        }

        // First locate all unique functions in this EL
        Fvisitor fv = new Fvisitor();
        el.visit(fv);
        ArrayList<ELNodeFunction> functions = fv.funcs;

        if (functions.size() == 0) {
            return;
        }

        // Reuse a previous map if possible
        String decName = matchMap(functions);
        if (decName != null) {
            el.setMapName(decName);
            return;
        }
    
        // Generate declaration for the map statically
        decName = getMapName();
        this.elFunctionMapper.getSs().append("private static org.apache.jasper.runtime.ProtectedFunctionMapper " + decName + ";\n");

        this.elFunctionMapper.getDs().append("  " + decName + "= ");
        this.elFunctionMapper.getDs().append("org.apache.jasper.runtime.ProtectedFunctionMapper");

        // Special case if there is only one function in the map
        String funcMethod = null;
        if (functions.size() == 1) {
            funcMethod = ".getMapForFunction";
        } else {
            this.elFunctionMapper.getDs().append(".getInstance();\n");
            funcMethod = "  " + decName + ".mapFunction";
        }

        // Setup arguments for either getMapForFunction or mapFunction
        for (int i = 0; i < functions.size(); i++) {
            ELNodeFunction f = functions.get(i);
            FunctionInfo funcInfo = f.getFunctionInfo();
            String key = f.getPrefix()+ ":" + f.getName();
            this.elFunctionMapper.getDs().append(funcMethod + "(\"" + key + "\", " +
                    getCanonicalName(funcInfo.getFunctionClass()) +
                    ".class, " + '\"' + f.getMethodName() + "\", " +
                    "new Class[] {");
            String params[] = f.getParameters();
            for (int k = 0; k < params.length; k++) {
                if (k != 0) {
                    this.elFunctionMapper.getDs().append(", ");
                }
                int iArray = params[k].indexOf('[');
                if (iArray < 0) {
                    this.elFunctionMapper.getDs().append(params[k] + ".class");
                }
                else {
                    String baseType = params[k].substring(0, iArray);
                    this.elFunctionMapper.getDs().append("java.lang.reflect.Array.newInstance(");
                    this.elFunctionMapper.getDs().append(baseType);
                    this.elFunctionMapper.getDs().append(".class,");

                    // Count the number of array dimension
                    int aCount = 0;
                    for (int jj = iArray; jj < params[k].length(); jj++ ) {
                        if (params[k].charAt(jj) == '[') {
                            aCount++;
                        }
                    }
                    if (aCount == 1) {
                        this.elFunctionMapper.getDs().append("0).getClass()");
                    } else {
                        this.elFunctionMapper.getDs().append("new int[" + aCount + "]).getClass()");
                    }
                }
            }
            this.elFunctionMapper.getDs().append("});\n");
            // Put the current name in the global function map
            gMap.put(f.getPrefix() + ':' + f.getName() + ':' + f.getUri(),
                     decName);
        }
        el.setMapName(decName);
    }

    /**
     * Find the name of the function mapper for an EL.  Reuse a
     * previously generated one if possible.
     * @param functions An ArrayList of ELNodeFunction instances that
     *                  represents the functions in an EL
     * @return A previous generated function mapper name that can be used
     *         by this EL; null if none found.
     */
    private String matchMap(ArrayList<ELNodeFunction> functions) {

        String mapName = null;
        for (int i = 0; i < functions.size(); i++) {
            ELNodeFunction f = functions.get(i);
            String temName = gMap.get(f.getPrefix() + ':' + f.getName() +
                    ':' + f.getUri());
            if (temName == null) {
                return null;
            }
            if (mapName == null) {
                mapName = temName;
            } else if (!temName.equals(mapName)) {
                // If not all in the previous match, then no match.
                return null;
            }
        }
        return mapName;
    }

    /*
     * @return An unique name for a function mapper.
     */
    private String getMapName() {
    	int aux = this.elFunctionMapper.getCurrFunc();
    	this.elFunctionMapper.setCurrFunc(this.elFunctionMapper.getCurrFunc()+1);
        return "_jspx_fnmap_" + aux;
    }

    /**
     * Convert a binary class name into a canonical one that can be used
     * when generating Java source code.
     * 
     * @param className Binary class name
     * @return          Canonical equivalent
     */
    private String getCanonicalName(String className) throws JasperException {
        Class<?> clazz;
        
        ClassLoader tccl;
        if (Constants28.isSecurityEnabled()) {
            PrivilegedAction<ClassLoader> pa = new PrivilegedGetTccl();
            tccl = AccessController.doPrivileged(pa);
        } else {
            tccl = Thread.currentThread().getContextClassLoader();
        }

        try {
            clazz = Class.forName(className, false, tccl);
        } catch (ClassNotFoundException e) {
            throw new JasperException(e);
        }
        return clazz.getCanonicalName();
    }
}