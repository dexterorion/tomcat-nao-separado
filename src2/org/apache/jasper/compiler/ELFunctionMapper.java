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

import org.apache.jasper.JasperException;

/**
 * This class generates functions mappers for the EL expressions in the page.
 * Instead of a global mapper, a mapper is used for each call to EL evaluator,
 * thus avoiding the prefix overlapping and redefinition issues.
 *
 * @author Kin-man Chung
 */

public class ELFunctionMapper {
	private int currFunc = 0;
	private StringBuilder ds; // Contains codes to initialize the functions
								// mappers.
	private StringBuilder ss; // Contains declarations of the functions mappers.

	/**
	 * Creates the functions mappers for all EL expressions in the JSP page.
	 *
	 * @param page
	 *            The current compilation unit.
	 */
	public static void map(NodeNodes page) throws JasperException {

		ELFunctionMapper map = new ELFunctionMapper();
		map.setDsData(new StringBuilder());
		map.setSsData(new StringBuilder());

		page.visit(new ElFunctionMapperELFunctionVisitor(map));

		// Append the declarations to the root node
		String ds = map.getDsData().toString();
		if (ds.length() > 0) {
			Node root = page.getRoot();
			new NodeDeclaration(map.getSsData().toString(), null, root);
			new NodeDeclaration("static {\n" + ds + "}\n", null, root);
		}
	}

	public int getCurrFunc() {
		return getCurrFuncData();
	}

	public void setCurrFunc(int currFunc) {
		this.setCurrFuncData(currFunc);
	}

	public StringBuilder getDs() {
		return getDsData();
	}

	public void setDs(StringBuilder ds) {
		this.setDsData(ds);
	}

	public StringBuilder getSs() {
		return getSsData();
	}

	public void setSs(StringBuilder ss) {
		this.setSsData(ss);
	}

	public int getCurrFuncData() {
		return currFunc;
	}

	public void setCurrFuncData(int currFunc) {
		this.currFunc = currFunc;
	}

	public StringBuilder getDsData() {
		return ds;
	}

	public void setDsData(StringBuilder ds) {
		this.ds = ds;
	}

	public StringBuilder getSsData() {
		return ss;
	}

	public void setSsData(StringBuilder ss) {
		this.ss = ss;
	}

}
