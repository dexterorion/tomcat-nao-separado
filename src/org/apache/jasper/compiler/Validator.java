/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jasper.compiler;

import java.util.Iterator;

import javax.servlet.jsp.tagext.PageData;
import javax.servlet.jsp.tagext.TagLibraryInfo;
import javax.servlet.jsp.tagext.ValidationMessage;

import org.apache.jasper.JasperException;

/**
 * Performs validation on the page elements. Attributes are checked for
 * mandatory presence, entry value validity, and consistency. As a side effect,
 * some page global value (such as those from page directives) are stored, for
 * later use.
 *
 * @author Kin-man Chung
 * @author Jan Luehe
 * @author Shawn Bayern
 * @author Mark Roth
 */
public class Validator {

    public static void validateDirectives(Compiler2 compiler, NodeNodes page)
            throws JasperException {
        page.visit(new ValidatorDirectiveVisitor(compiler));
    }

    public static void validateExDirectives(Compiler2 compiler, NodeNodes page)
        throws JasperException {
        // Determine the default output content type
        PageInfo pageInfo = compiler.getPageInfo();
        String contentType = pageInfo.getContentType();

        if (contentType == null || contentType.indexOf("charset=") < 0) {
            boolean isXml = page.getRoot().isXmlSyntax();
            String defaultType;
            if (contentType == null) {
                defaultType = isXml ? "text/xml" : "text/html";
            } else {
                defaultType = contentType;
            }

            String charset = null;
            if (isXml) {
                charset = "UTF-8";
            } else {
                if (!page.getRoot().isDefaultPageEncoding()) {
                    charset = page.getRoot().getPageEncoding();
                }
            }

            if (charset != null) {
                pageInfo.setContentType(defaultType + ";charset=" + charset);
            } else {
                pageInfo.setContentType(defaultType);
            }
        }

        /*
         * Validate all other nodes. This validation step includes checking a
         * custom tag's mandatory and optional attributes against information in
         * the TLD (first validation step for custom tags according to
         * JSP.10.5).
         */
        page.visit(new ValidatorValidateVisitor(compiler));

        /*
         * Invoke TagLibraryValidator classes of all imported tags (second
         * validation step for custom tags according to JSP.10.5).
         */
        validateXmlView(new PageDataImpl(page, compiler), compiler);

        /*
         * Invoke TagExtraInfo method isValid() for all imported tags (third
         * validation step for custom tags according to JSP.10.5).
         */
        page.visit(new ValidatorTagExtraInfoVisitor(compiler));

    }

    // *********************************************************************
    // Private (utility) methods

    /**
     * Validate XML view against the TagLibraryValidator classes of all imported
     * tag libraries.
     */
    private static void validateXmlView(PageData xmlView, Compiler2 compiler)
            throws JasperException {

        StringBuilder errMsg = null;
        ErrorDispatcher errDisp = compiler.getErrorDispatcher();

        for (Iterator<TagLibraryInfo> iter =
            compiler.getPageInfo().getTaglibs().iterator(); iter.hasNext();) {

            Object o = iter.next();
            if (!(o instanceof TagLibraryInfoImpl))
                continue;
            TagLibraryInfoImpl tli = (TagLibraryInfoImpl) o;

            ValidationMessage[] errors = tli.validate(xmlView);
            if ((errors != null) && (errors.length != 0)) {
                if (errMsg == null) {
                    errMsg = new StringBuilder();
                }
                errMsg.append("<h3>");
                errMsg.append(Localizer.getMessage(
                        "jsp.error.tlv.invalid.page", tli.getShortName(),
                        compiler.getPageInfo().getJspFile()));
                errMsg.append("</h3>");
                for (int i = 0; i < errors.length; i++) {
                    if (errors[i] != null) {
                        errMsg.append("<p>");
                        errMsg.append(errors[i].getId());
                        errMsg.append(": ");
                        errMsg.append(errors[i].getMessage());
                        errMsg.append("</p>");
                    }
                }
            }
        }

        if (errMsg != null) {
            errDisp.jspError(errMsg.toString());
        }
    }

    protected static String xmlEscape(String s) {
        if (s == null) {
            return null;
        }
        int len = s.length();

        /*
         * Look for any "bad" characters, Escape "bad" character was found
         */
        // ASCII " 34 & 38 ' 39 < 60 > 62
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c >= '\"' && c <= '>' &&
                    (c == '<' || c == '>' || c == '\'' || c == '&' || c == '"')) {
                // need to escape them and then quote the whole string
                StringBuilder sb = new StringBuilder((int) (len * 1.2));
                sb.append(s, 0, i);
                int pos = i + 1;
                for (int j = i; j < len; j++) {
                    c = s.charAt(j);
                    if (c >= '\"' && c <= '>') {
                        if (c == '<') {
                            if (j > pos) {
                                sb.append(s, pos, j);
                            }
                            sb.append("&lt;");
                            pos = j + 1;
                        } else if (c == '>') {
                            if (j > pos) {
                                sb.append(s, pos, j);
                            }
                            sb.append("&gt;");
                            pos = j + 1;
                        } else if (c == '\'') {
                            if (j > pos) {
                                sb.append(s, pos, j);
                            }
                            sb.append("&#039;"); // &apos;
                            pos = j + 1;
                        } else if (c == '&') {
                            if (j > pos) {
                                sb.append(s, pos, j);
                            }
                            sb.append("&amp;");
                            pos = j + 1;
                        } else if (c == '"') {
                            if (j > pos) {
                                sb.append(s, pos, j);
                            }
                            sb.append("&#034;"); // &quot;
                            pos = j + 1;
                        }
                    }
                }
                if (pos < len) {
                    sb.append(s, pos, len);
                }
                return sb.toString();
            }
        }
        return s;
    }
}
