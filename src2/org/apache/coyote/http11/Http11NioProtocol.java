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

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.net.AbstractEndpointHandler;
import org.apache.tomcat.util.net.NioChannel;
import org.apache.tomcat.util.net.NioEndpoint;


/**
 * Abstract the protocol implementation, including threading, etc.
 * Processor is single threaded and specific to stream-based protocols,
 * will not fit Jk protocols like JNI.
 *
 * @author Remy Maucherat
 * @author Costin Manolache
 * @author Filip Hanik
 */
public class Http11NioProtocol extends AbstractHttp11JsseProtocol<NioChannel> {

    private static final Log log = LogFactory.getLog(Http11NioProtocol.class);


    @Override
    protected Log getLog() { return log; }


    @Override
    protected AbstractEndpointHandler getHandler() {
        return cHandler;
    }


    public Http11NioProtocol() {
        setEndpoint(new NioEndpoint());
        cHandler = new Http11NioProtocolHttp11ConnectionHandler(this);
        ((NioEndpoint) getEndpoint()).setHandler(cHandler);
        setSoLinger(Constants26.getDefaultConnectionLinger());
        setSoTimeout(Constants26.getDefaultConnectionTimeout());
        setTcpNoDelay(Constants26.isDefaultTcpNoDelay());
    }


    public NioEndpoint getEndpoint() {
        return ((NioEndpoint)getEndpoint());
    }


    // -------------------- Properties--------------------

    private Http11NioProtocolHttp11ConnectionHandler cHandler;

    // -------------------- Pool setup --------------------

    public void setPollerThreadCount(int count) {
        ((NioEndpoint)getEndpoint()).setPollerThreadCount(count);
    }

    public int getPollerThreadCount() {
        return ((NioEndpoint)getEndpoint()).getPollerThreadCount();
    }

    public void setSelectorTimeout(long timeout) {
        ((NioEndpoint)getEndpoint()).setSelectorTimeout(timeout);
    }

    public long getSelectorTimeout() {
        return ((NioEndpoint)getEndpoint()).getSelectorTimeout();
    }

    public void setAcceptorThreadPriority(int threadPriority) {
        ((NioEndpoint)getEndpoint()).setAcceptorThreadPriority(threadPriority);
    }

    public void setPollerThreadPriority(int threadPriority) {
        ((NioEndpoint)getEndpoint()).setPollerThreadPriority(threadPriority);
    }

    public int getAcceptorThreadPriority() {
      return ((NioEndpoint)getEndpoint()).getAcceptorThreadPriority();
    }

    public int getPollerThreadPriority() {
      return ((NioEndpoint)getEndpoint()).getThreadPriority();
    }


    public boolean getUseSendfile() {
        return ((NioEndpoint)getEndpoint()).getUseSendfile();
    }

    public void setUseSendfile(boolean useSendfile) {
        ((NioEndpoint)getEndpoint()).setUseSendfile(useSendfile);
    }

    // -------------------- Tcp setup --------------------
    public void setOomParachute(int oomParachute) {
        ((NioEndpoint)getEndpoint()).setOomParachute(oomParachute);
    }

    // ----------------------------------------------------- JMX related methods

    @Override
    protected String getNamePrefix() {
        return ("http-nio");
    }
    
    public static Log getLogVariable(){
    	return log;
    }
}

