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
 * Collect info about the page and nodes, and make them available through
 * the PageInfo object.
 *
 * @author Kin-man Chung
 * @author Mark Roth
 */

public class Collector {

    public static void collect(Compiler2 compiler, NodeNodes page)
        throws JasperException {

    	CollectorCollectVisitor collectVisitor = new CollectorCollectVisitor();
        page.visit(collectVisitor);
        collectVisitor.updatePageInfo(compiler.getPageInfo());

    }
}
