package org.apache.jasper.compiler;

import java.util.Vector;

public class JspConfigJspProperty {

    private String isXml;
    private String elIgnored;
    private String scriptingInvalid;
    private String pageEncoding;
    private Vector<String> includePrelude;
    private Vector<String> includeCoda;
    private String deferedSyntaxAllowedAsLiteral;
    private String trimDirectiveWhitespaces;
    private String defaultContentType;
    private String buffer;
    private String errorOnUndeclaredNamespace;

    public JspConfigJspProperty(String isXml, String elIgnored,
            String scriptingInvalid, String pageEncoding,
            Vector<String> includePrelude, Vector<String> includeCoda,
            String deferedSyntaxAllowedAsLiteral, 
            String trimDirectiveWhitespaces,
            String defaultContentType,
            String buffer,
            String errorOnUndeclaredNamespace) {

        this.isXml = isXml;
        this.elIgnored = elIgnored;
        this.scriptingInvalid = scriptingInvalid;
        this.pageEncoding = pageEncoding;
        this.includePrelude = includePrelude;
        this.includeCoda = includeCoda;
        this.deferedSyntaxAllowedAsLiteral = deferedSyntaxAllowedAsLiteral;
        this.trimDirectiveWhitespaces = trimDirectiveWhitespaces;
        this.defaultContentType = defaultContentType;
        this.buffer = buffer;
        this.errorOnUndeclaredNamespace = errorOnUndeclaredNamespace;
    }

    public String isXml() {
        return isXml;
    }

    public String isELIgnored() {
        return elIgnored;
    }

    public String isScriptingInvalid() {
        return scriptingInvalid;
    }

    public String getPageEncoding() {
        return pageEncoding;
    }

    public Vector<String> getIncludePrelude() {
        return includePrelude;
    }

    public Vector<String> getIncludeCoda() {
        return includeCoda;
    }
    
    public String isDeferedSyntaxAllowedAsLiteral() {
        return deferedSyntaxAllowedAsLiteral;
    }
    
    public String isTrimDirectiveWhitespaces() {
        return trimDirectiveWhitespaces;
    }
    
    public String getDefaultContentType() {
        return defaultContentType;
    }
    
    public String getBuffer() {
        return buffer;
    }
    
    public String isErrorOnUndeclaredNamespace() {
        return errorOnUndeclaredNamespace;
    }
}