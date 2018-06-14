package org.apache.jasper.compiler;

import java.util.Locale;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

/**
 * A visitor to validate and extract page directive info
 */
public class ValidatorDirectiveVisitor extends NodeVisitor {

    private PageInfo pageInfo;

    private ErrorDispatcher err;

    private static final JspUtilValidAttribute[] pageDirectiveAttrs = {
        new JspUtilValidAttribute("language"),
        new JspUtilValidAttribute("extends"),
        new JspUtilValidAttribute("import"),
        new JspUtilValidAttribute("session"),
        new JspUtilValidAttribute("buffer"),
        new JspUtilValidAttribute("autoFlush"),
        new JspUtilValidAttribute("isThreadSafe"),
        new JspUtilValidAttribute("info"),
        new JspUtilValidAttribute("errorPage"),
        new JspUtilValidAttribute("isErrorPage"),
        new JspUtilValidAttribute("contentType"),
        new JspUtilValidAttribute("pageEncoding"),
        new JspUtilValidAttribute("isELIgnored"),
        new JspUtilValidAttribute("deferredSyntaxAllowedAsLiteral"),
        new JspUtilValidAttribute("trimDirectiveWhitespaces")
    };

    private boolean pageEncodingSeen = false;

    /*
     * Constructor
     */
    public ValidatorDirectiveVisitor(Compiler2 compiler) {
        this.pageInfo = compiler.getPageInfo();
        this.err = compiler.getErrorDispatcher();
    }

    @Override
    public void visit(NodeIncludeDirective n) throws JasperException {
        // Since pageDirectiveSeen flag only applies to the Current page
        // save it here and restore it after the file is included.
        boolean pageEncodingSeenSave = pageEncodingSeen;
        pageEncodingSeen = false;
        visitBody(n);
        pageEncodingSeen = pageEncodingSeenSave;
    }

    @Override
    public void visit(NodePageDirective n) throws JasperException {

        JspUtil.checkAttributes("Page directive", n, pageDirectiveAttrs,
                err);

        // JSP.2.10.1
        Attributes attrs = n.getAttributes();
        for (int i = 0; attrs != null && i < attrs.getLength(); i++) {
            String attr = attrs.getQName(i);
            String value = attrs.getValue(i);

            if ("language".equals(attr)) {
                if (pageInfo.getLanguage(false) == null) {
                    pageInfo.setLanguage(value, n, err, true);
                } else if (!pageInfo.getLanguage(false).equals(value)) {
                    err.jspError(n, "jsp.error.page.conflict.language",
                            pageInfo.getLanguage(false), value);
                }
            } else if ("extends".equals(attr)) {
                if (pageInfo.getExtends(false) == null) {
                    pageInfo.setExtends(value);
                } else if (!pageInfo.getExtends(false).equals(value)) {
                    err.jspError(n, "jsp.error.page.conflict.extends",
                            pageInfo.getExtends(false), value);
                }
            } else if ("contentType".equals(attr)) {
                if (pageInfo.getContentType() == null) {
                    pageInfo.setContentType(value);
                } else if (!pageInfo.getContentType().equals(value)) {
                    err.jspError(n, "jsp.error.page.conflict.contenttype",
                            pageInfo.getContentType(), value);
                }
            } else if ("session".equals(attr)) {
                if (pageInfo.getSession() == null) {
                    pageInfo.setSession(value, n, err);
                } else if (!pageInfo.getSession().equals(value)) {
                    err.jspError(n, "jsp.error.page.conflict.session",
                            pageInfo.getSession(), value);
                }
            } else if ("buffer".equals(attr)) {
                if (pageInfo.getBufferValue() == null) {
                    pageInfo.setBufferValue(value, n, err);
                } else if (!pageInfo.getBufferValue().equals(value)) {
                    err.jspError(n, "jsp.error.page.conflict.buffer",
                            pageInfo.getBufferValue(), value);
                }
            } else if ("autoFlush".equals(attr)) {
                if (pageInfo.getAutoFlush() == null) {
                    pageInfo.setAutoFlush(value, n, err);
                } else if (!pageInfo.getAutoFlush().equals(value)) {
                    err.jspError(n, "jsp.error.page.conflict.autoflush",
                            pageInfo.getAutoFlush(), value);
                }
            } else if ("isThreadSafe".equals(attr)) {
                if (pageInfo.getIsThreadSafe() == null) {
                    pageInfo.setIsThreadSafe(value, n, err);
                } else if (!pageInfo.getIsThreadSafe().equals(value)) {
                    err.jspError(n, "jsp.error.page.conflict.isthreadsafe",
                            pageInfo.getIsThreadSafe(), value);
                }
            } else if ("isELIgnored".equals(attr)) {
                if (pageInfo.getIsELIgnored() == null) {
                    pageInfo.setIsELIgnored(value, n, err, true);
                } else if (!pageInfo.getIsELIgnored().equals(value)) {
                    err.jspError(n, "jsp.error.page.conflict.iselignored",
                            pageInfo.getIsELIgnored(), value);
                }
            } else if ("isErrorPage".equals(attr)) {
                if (pageInfo.getIsErrorPage() == null) {
                    pageInfo.setIsErrorPage(value, n, err);
                } else if (!pageInfo.getIsErrorPage().equals(value)) {
                    err.jspError(n, "jsp.error.page.conflict.iserrorpage",
                            pageInfo.getIsErrorPage(), value);
                }
            } else if ("errorPage".equals(attr)) {
                if (pageInfo.getErrorPage() == null) {
                    pageInfo.setErrorPage(value);
                } else if (!pageInfo.getErrorPage().equals(value)) {
                    err.jspError(n, "jsp.error.page.conflict.errorpage",
                            pageInfo.getErrorPage(), value);
                }
            } else if ("info".equals(attr)) {
                if (pageInfo.getInfo() == null) {
                    pageInfo.setInfo(value);
                } else if (!pageInfo.getInfo().equals(value)) {
                    err.jspError(n, "jsp.error.page.conflict.info",
                            pageInfo.getInfo(), value);
                }
            } else if ("pageEncoding".equals(attr)) {
                if (pageEncodingSeen)
                    err.jspError(n, "jsp.error.page.multi.pageencoding");
                // 'pageEncoding' can occur at most once per file
                pageEncodingSeen = true;
                String actual = comparePageEncodings(value, n);
                n.getRoot().setPageEncoding(actual);
            } else if ("deferredSyntaxAllowedAsLiteral".equals(attr)) {
                if (pageInfo.getDeferredSyntaxAllowedAsLiteral() == null) {
                    pageInfo.setDeferredSyntaxAllowedAsLiteral(value, n,
                            err, true);
                } else if (!pageInfo.getDeferredSyntaxAllowedAsLiteral()
                        .equals(value)) {
                    err
                            .jspError(
                                    n,
                                    "jsp.error.page.conflict.deferredsyntaxallowedasliteral",
                                    pageInfo
                                            .getDeferredSyntaxAllowedAsLiteral(),
                                    value);
                }
            } else if ("trimDirectiveWhitespaces".equals(attr)) {
                if (pageInfo.getTrimDirectiveWhitespaces() == null) {
                    pageInfo.setTrimDirectiveWhitespaces(value, n, err,
                            true);
                } else if (!pageInfo.getTrimDirectiveWhitespaces().equals(
                        value)) {
                    err
                            .jspError(
                                    n,
                                    "jsp.error.page.conflict.trimdirectivewhitespaces",
                                    pageInfo.getTrimDirectiveWhitespaces(),
                                    value);
                }
            }
        }

        // Check for bad combinations
        if (pageInfo.getBuffer() == 0 && !pageInfo.isAutoFlush())
            err.jspError(n, "jsp.error.page.badCombo");

        // Attributes for imports for this node have been processed by
        // the parsers, just add them to pageInfo.
        pageInfo.addImports(n.getImports());
    }

    @Override
    public void visit(NodeTagDirective n) throws JasperException {
        // Note: Most of the validation is done in TagFileProcessor
        // when it created a TagInfo object from the
        // tag file in which the directive appeared.

        // This method does additional processing to collect page info

        Attributes attrs = n.getAttributes();
        for (int i = 0; attrs != null && i < attrs.getLength(); i++) {
            String attr = attrs.getQName(i);
            String value = attrs.getValue(i);

            if ("language".equals(attr)) {
                if (pageInfo.getLanguage(false) == null) {
                    pageInfo.setLanguage(value, n, err, false);
                } else if (!pageInfo.getLanguage(false).equals(value)) {
                    err.jspError(n, "jsp.error.tag.conflict.language",
                            pageInfo.getLanguage(false), value);
                }
            } else if ("isELIgnored".equals(attr)) {
                if (pageInfo.getIsELIgnored() == null) {
                    pageInfo.setIsELIgnored(value, n, err, false);
                } else if (!pageInfo.getIsELIgnored().equals(value)) {
                    err.jspError(n, "jsp.error.tag.conflict.iselignored",
                            pageInfo.getIsELIgnored(), value);
                }
            } else if ("pageEncoding".equals(attr)) {
                if (pageEncodingSeen)
                    err.jspError(n, "jsp.error.tag.multi.pageencoding");
                pageEncodingSeen = true;
                compareTagEncodings(value, n);
                n.getRoot().setPageEncoding(value);
            } else if ("deferredSyntaxAllowedAsLiteral".equals(attr)) {
                if (pageInfo.getDeferredSyntaxAllowedAsLiteral() == null) {
                    pageInfo.setDeferredSyntaxAllowedAsLiteral(value, n,
                            err, false);
                } else if (!pageInfo.getDeferredSyntaxAllowedAsLiteral()
                        .equals(value)) {
                    err
                            .jspError(
                                    n,
                                    "jsp.error.tag.conflict.deferredsyntaxallowedasliteral",
                                    pageInfo
                                            .getDeferredSyntaxAllowedAsLiteral(),
                                    value);
                }
            } else if ("trimDirectiveWhitespaces".equals(attr)) {
                if (pageInfo.getTrimDirectiveWhitespaces() == null) {
                    pageInfo.setTrimDirectiveWhitespaces(value, n, err,
                            false);
                } else if (!pageInfo.getTrimDirectiveWhitespaces().equals(
                        value)) {
                    err
                            .jspError(
                                    n,
                                    "jsp.error.tag.conflict.trimdirectivewhitespaces",
                                    pageInfo.getTrimDirectiveWhitespaces(),
                                    value);
                }
            }
        }

        // Attributes for imports for this node have been processed by
        // the parsers, just add them to pageInfo.
        pageInfo.addImports(n.getImports());
    }

    @Override
    public void visit(NodeAttributeDirective n) throws JasperException {
        // Do nothing, since this attribute directive has already been
        // validated by TagFileProcessor when it created a TagInfo object
        // from the tag file in which the directive appeared
    }

    @Override
    public void visit(NodeVariableDirective n) throws JasperException {
        // Do nothing, since this variable directive has already been
        // validated by TagFileProcessor when it created a TagInfo object
        // from the tag file in which the directive appeared
    }

    /*
     * Compares page encodings specified in various places, and throws
     * exception in case of page encoding mismatch.
     *
     * @param pageDirEnc The value of the pageEncoding attribute of the page
     * directive @param pageDir The page directive node
     *
     * @throws JasperException in case of page encoding mismatch
     */
    private String comparePageEncodings(String thePageDirEnc,
            NodePageDirective pageDir) throws JasperException {

        NodeRoot root = pageDir.getRoot();
        String configEnc = root.getJspConfigPageEncoding();
        String pageDirEnc = thePageDirEnc.toUpperCase(Locale.ENGLISH);

        /*
         * Compare the 'pageEncoding' attribute of the page directive with
         * the encoding specified in the JSP config element whose URL
         * pattern matches this page. Treat "UTF-16", "UTF-16BE", and
         * "UTF-16LE" as identical.
         */
        if (configEnc != null) {
            configEnc = configEnc.toUpperCase(Locale.ENGLISH);
            if (!pageDirEnc.equals(configEnc)
                    && (!pageDirEnc.startsWith("UTF-16") || !configEnc
                            .startsWith("UTF-16"))) {
                err.jspError(pageDir,
                        "jsp.error.config_pagedir_encoding_mismatch",
                        configEnc, pageDirEnc);
            } else {
                return configEnc;
            }
        }

        /*
         * Compare the 'pageEncoding' attribute of the page directive with
         * the encoding specified in the XML prolog (only for XML syntax,
         * and only if JSP document contains XML prolog with encoding
         * declaration). Treat "UTF-16", "UTF-16BE", and "UTF-16LE" as
         * identical.
         */
        if ((root.isXmlSyntax() && root.isEncodingSpecifiedInProlog()) || root.isBomPresent()) {
            String pageEnc = root.getPageEncoding().toUpperCase(Locale.ENGLISH);
            if (!pageDirEnc.equals(pageEnc)
                    && (!pageDirEnc.startsWith("UTF-16") || !pageEnc
                            .startsWith("UTF-16"))) {
                err.jspError(pageDir,
                        "jsp.error.prolog_pagedir_encoding_mismatch",
                        pageEnc, pageDirEnc);
            } else {
                return pageEnc;
            }
        }

        return pageDirEnc;
    }

    /*
     * Compares page encodings specified in various places, and throws
     * exception in case of page encoding mismatch.
     *
     * @param thePageDirEnc The value of the pageEncoding attribute of the page
     * directive @param pageDir The page directive node
     *
     * @throws JasperException in case of page encoding mismatch
     */
    private void compareTagEncodings(String thePageDirEnc,
            NodeTagDirective pageDir) throws JasperException {

        NodeRoot root = pageDir.getRoot();
        String pageDirEnc = thePageDirEnc.toUpperCase(Locale.ENGLISH);
        /*
         * Compare the 'pageEncoding' attribute of the page directive with
         * the encoding specified in the XML prolog (only for XML syntax,
         * and only if JSP document contains XML prolog with encoding
         * declaration). Treat "UTF-16", "UTF-16BE", and "UTF-16LE" as
         * identical.
         */
        if ((root.isXmlSyntax() && root.isEncodingSpecifiedInProlog()) || root.isBomPresent()) {
            String pageEnc = root.getPageEncoding().toUpperCase(Locale.ENGLISH);
            if (!pageDirEnc.equals(pageEnc)
                    && (!pageDirEnc.startsWith("UTF-16") || !pageEnc
                            .startsWith("UTF-16"))) {
                err.jspError(pageDir,
                        "jsp.error.prolog_pagedir_encoding_mismatch",
                        pageEnc, pageDirEnc);
            }
        }
    }

}