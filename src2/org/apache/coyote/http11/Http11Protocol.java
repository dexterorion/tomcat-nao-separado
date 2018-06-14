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
package org.apache.coyote.http11;

import java.net.Socket;

import org.apache.juli.logging.Log;
import org.apache.tomcat.util.net.AbstractEndpointHandler;
import org.apache.tomcat.util.net.JIoEndpoint;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;


/**
 * Abstract the protocol implementation, including threading, etc.
 * Processor is single threaded and specific to stream-based protocols,
 * will not fit Jk protocols like JNI.
 *
 * @author Remy Maucherat
 * @author Costin Manolache
 */
public class Http11Protocol extends AbstractHttp11JsseProtocol<Socket> {


    private static final Log log = LogFactory.getLog(Http11Protocol.class);
    
    @Override
    protected Log getLog() { return log; }


    @Override
    protected AbstractEndpointHandler getHandler() {
        return cHandler;
    }


    // ------------------------------------------------------------ Constructor


    public Http11Protocol() {
        setEndpoint(new JIoEndpoint());
        cHandler = new Http11ProtocolHttp11ConnectionHandler(this);
        ((JIoEndpoint) getEndpoint()).setHandler(cHandler);
        setSoLinger(Constants26.getDefaultConnectionLinger());
        setSoTimeout(Constants26.getDefaultConnectionTimeout());
        setTcpNoDelay(Constants26.isDefaultTcpNoDelay());
    }

    
    // ----------------------------------------------------------------- Fields

    private Http11ProtocolHttp11ConnectionHandler cHandler;


    // ------------------------------------------------ HTTP specific properties
    // ------------------------------------------ managed in the ProtocolHandler

    private int disableKeepAlivePercentage = 75;
    public int getDisableKeepAlivePercentage() {
        return disableKeepAlivePercentage;
    }
    public void setDisableKeepAlivePercentage(int disableKeepAlivePercentage) {
        if (disableKeepAlivePercentage < 0) {
            this.disableKeepAlivePercentage = 0;
        } else if (disableKeepAlivePercentage > 100) {
            this.disableKeepAlivePercentage = 100;
        } else {
            this.disableKeepAlivePercentage = disableKeepAlivePercentage;
        }
    }
    
    // ----------------------------------------------------- JMX related methods

    @Override
    protected String getNamePrefix() {
        return ("http-bio");
    }
    
    public static Log getLogVariable(){
    	return log;
    }
}
