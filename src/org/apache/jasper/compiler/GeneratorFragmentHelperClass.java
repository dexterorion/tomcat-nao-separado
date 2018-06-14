package org.apache.jasper.compiler;

import java.util.ArrayList;

import org.apache.jasper.JasperException;

public class GeneratorFragmentHelperClass {

    // True if the helper class should be generated.
    private boolean used = false;

    private ArrayList<GeneratorFragmentHelperClassFragment> fragments = new ArrayList<GeneratorFragmentHelperClassFragment>();

    private String className;

    // Buffer for entire helper class
    private GeneratorGenBuffer classBuffer = new GeneratorGenBuffer();

    public GeneratorFragmentHelperClass(String className) {
        this.className = className;
    }

    public String getClassName() {
        return this.className;
    }

    public boolean isUsed() {
        return this.used;
    }

    public void generatePreamble() {
        ServletWriter out = this.classBuffer.getOut();
        out.println();
        out.pushIndent();
        // Note: cannot be static, as we need to reference things like
        // _jspx_meth_*
        out.printil("private class " + className);
        out.printil("    extends "
                + "org.apache.jasper.runtime.JspFragmentHelper");
        out.printil("{");
        out.pushIndent();
        out.printil("private javax.servlet.jsp.tagext.JspTag _jspx_parent;");
        out.printil("private int[] _jspx_push_body_count;");
        out.println();
        out.printil("public " + className
                + "( int discriminator, javax.servlet.jsp.JspContext jspContext, "
                + "javax.servlet.jsp.tagext.JspTag _jspx_parent, "
                + "int[] _jspx_push_body_count ) {");
        out.pushIndent();
        out.printil("super( discriminator, jspContext, _jspx_parent );");
        out.printil("this._jspx_parent = _jspx_parent;");
        out.printil("this._jspx_push_body_count = _jspx_push_body_count;");
        out.popIndent();
        out.printil("}");
    }

    public GeneratorFragmentHelperClassFragment openFragment(Node parent, int methodNesting)
    throws JasperException {
        GeneratorFragmentHelperClassFragment result = new GeneratorFragmentHelperClassFragment(fragments.size(), parent);
        fragments.add(result);
        this.used = true;
        parent.setInnerClassName(className);

        ServletWriter out = result.getGeneratorGenBuffer().getOut();
        out.pushIndent();
        out.pushIndent();
        // XXX - Returns boolean because if a tag is invoked from
        // within this fragment, the Generator sometimes might
        // generate code like "return true". This is ignored for now,
        // meaning only the fragment is skipped. The JSR-152
        // expert group is currently discussing what to do in this case.
        // See comment in closeFragment()
        if (methodNesting > 0) {
            out.printin("public boolean invoke");
        } else {
            out.printin("public void invoke");
        }
        out.println(result.getId() + "( " + "javax.servlet.jsp.JspWriter out ) ");
        out.pushIndent();
        // Note: Throwable required because methods like _jspx_meth_*
        // throw Throwable.
        out.printil("throws java.lang.Throwable");
        out.popIndent();
        out.printil("{");
        out.pushIndent();
        Generator.generateLocalVariables(out, parent);

        return result;
    }

    public void closeFragment(GeneratorFragmentHelperClassFragment fragment, int methodNesting) {
        ServletWriter out = fragment.getGeneratorGenBuffer().getOut();
        // XXX - See comment in openFragment()
        if (methodNesting > 0) {
            out.printil("return false;");
        } else {
            out.printil("return;");
        }
        out.popIndent();
        out.printil("}");
    }

    public void generatePostamble() {
        ServletWriter out = this.classBuffer.getOut();
        // Generate all fragment methods:
        for (int i = 0; i < fragments.size(); i++) {
            GeneratorFragmentHelperClassFragment fragment = fragments.get(i);
            fragment.getGeneratorGenBuffer().adjustJavaLines(out.getJavaLine() - 1);
            out.printMultiLn(fragment.getGeneratorGenBuffer().toString());
        }

        // Generate postamble:
        out.printil("public void invoke( java.io.Writer writer )");
        out.pushIndent();
        out.printil("throws javax.servlet.jsp.JspException");
        out.popIndent();
        out.printil("{");
        out.pushIndent();
        out.printil("javax.servlet.jsp.JspWriter out = null;");
        out.printil("if( writer != null ) {");
        out.pushIndent();
        out.printil("out = this.jspContext.pushBody(writer);");
        out.popIndent();
        out.printil("} else {");
        out.pushIndent();
        out.printil("out = this.jspContext.getOut();");
        out.popIndent();
        out.printil("}");
        out.printil("try {");
        out.pushIndent();
        out.printil("Object _jspx_saved_JspContext = this.jspContext.getELContext().getContext(javax.servlet.jsp.JspContext.class);");
        out.printil("this.jspContext.getELContext().putContext(javax.servlet.jsp.JspContext.class,this.jspContext);");
        out.printil("switch( this.discriminator ) {");
        out.pushIndent();
        for (int i = 0; i < fragments.size(); i++) {
            out.printil("case " + i + ":");
            out.pushIndent();
            out.printil("invoke" + i + "( out );");
            out.printil("break;");
            out.popIndent();
        }
        out.popIndent();
        out.printil("}"); // switch

        // restore nested JspContext on ELContext
        out.printil("jspContext.getELContext().putContext(javax.servlet.jsp.JspContext.class,_jspx_saved_JspContext);");

        out.popIndent();
        out.printil("}"); // try
        out.printil("catch( java.lang.Throwable e ) {");
        out.pushIndent();
        out.printil("if (e instanceof javax.servlet.jsp.SkipPageException)");
        out.printil("    throw (javax.servlet.jsp.SkipPageException) e;");
        out.printil("throw new javax.servlet.jsp.JspException( e );");
        out.popIndent();
        out.printil("}"); // catch
        out.printil("finally {");
        out.pushIndent();

        out.printil("if( writer != null ) {");
        out.pushIndent();
        out.printil("this.jspContext.popBody();");
        out.popIndent();
        out.printil("}");

        out.popIndent();
        out.printil("}"); // finally
        out.popIndent();
        out.printil("}"); // invoke method
        out.popIndent();
        out.printil("}"); // helper class
        out.popIndent();
    }

    @Override
    public String toString() {
        return classBuffer.toString();
    }

    public void adjustJavaLines(int offset) {
        for (int i = 0; i < fragments.size(); i++) {
            GeneratorFragmentHelperClassFragment fragment = fragments.get(i);
            fragment.getGeneratorGenBuffer().adjustJavaLines(offset);
        }
    }
}