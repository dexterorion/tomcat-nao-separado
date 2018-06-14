/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.coyote.ajp;

import java.net.Socket;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.net.AbstractEndpointHandler;
import org.apache.tomcat.util.net.JIoEndpoint;


/**
 * Abstract the protocol implementation, including threading, etc.
 * Processor is single threaded and specific to stream-based protocols,
 * will not fit Jk protocols like JNI.
 *
 * @author Remy Maucherat
 * @author Costin Manolache
 */
public class AjpProtocol extends AbstractAjpProtocol<Socket> {
    
    
    private static final Log log = LogFactory.getLog(AjpProtocol.class);

    @Override
    protected Log getLog() { return log; }


    @Override
    protected AbstractEndpointHandler getHandler() {
        return cHandler;
    }


    // ------------------------------------------------------------ Constructor


    public AjpProtocol() {
        setEndpoint(new JIoEndpoint());
        cHandler = new AjpProtocolAjpConnectionHandler(this);
        ((JIoEndpoint) getEndpoint()).setHandler(cHandler);
        setSoLinger(Constants25.getDefaultConnectionLinger());
        setSoTimeout(Constants25.getDefaultConnectionTimeout());
        setTcpNoDelay(Constants25.isDefaultTcpNoDelay());
    }

    
    // ----------------------------------------------------- Instance Variables

    
    /**
     * Connection handler for AJP.
     */
    private AjpProtocolAjpConnectionHandler cHandler;


    // ----------------------------------------------------- JMX related methods

    @Override
    protected String getNamePrefix() {
        return ("ajp-bio");
    }
    
    public static Log getLogVariable(){
    	return log;
    }
}
