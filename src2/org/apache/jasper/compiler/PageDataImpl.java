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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.servlet.jsp.tagext.PageData;

import org.apache.jasper.JasperException;

/**
 * An implementation of <tt>javax.servlet.jsp.tagext.PageData</tt> which
 * builds the XML view of a given page.
 *
 * The XML view is built in two passes:
 *
 * During the first pass, the PageDataImplFirstPassVisitor collects the attributes of the
 * top-level jsp:root and those of the jsp:root elements of any included
 * pages, and adds them to the jsp:root element of the XML view.
 * In addition, any taglib directives are converted into xmlns: attributes and
 * added to the jsp:root element of the XML view.
 * This pass ignores any nodes other than JspRoot and TaglibDirective.
 *
 * During the second pass, the PageDataImplSecondPassVisitor produces the XML view, using
 * the combined jsp:root attributes determined in the first pass and any
 * remaining pages nodes (this pass ignores any JspRoot and TaglibDirective
 * nodes).
 *
 * @author Jan Luehe
 */
public class PageDataImpl extends PageData implements TagConstants {

    private static final String JSP_VERSION = "2.0";
    private static final String CDATA_START_SECTION = "<![CDATA[\n";
    private static final String CDATA_END_SECTION = "]]>\n";
    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    // string buffer used to build XML view
    private StringBuilder buf;

    /**
     * Constructor.
     *
     * @param page the page nodes from which to generate the XML view
     */
    public PageDataImpl(NodeNodes page, Compiler2 compiler)
                throws JasperException {

        // First pass
        PageDataImplFirstPassVisitor firstPass = new PageDataImplFirstPassVisitor(page.getRoot(),
                                                          compiler.getPageInfo());
        page.visit(firstPass);

        // Second pass
        buf = new StringBuilder();
        PageDataImplSecondPassVisitor secondPass
            = new PageDataImplSecondPassVisitor(page.getRoot(), buf, compiler,
                                    firstPass.getJspIdPrefix());
        page.visit(secondPass);
    }

    /**
     * Returns the input stream of the XML view.
     *
     * @return the input stream of the XML view
     */
    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(buf.toString().getBytes(CHARSET_UTF8));
    }

	public static String getJspVersion() {
		return JSP_VERSION;
	}

	public static String getCdataStartSection() {
		return CDATA_START_SECTION;
	}

	public static String getCdataEndSection() {
		return CDATA_END_SECTION;
	}
}

