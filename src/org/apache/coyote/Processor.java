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

package org.apache.coyote;

import java.io.IOException;
import java.util.concurrent.Executor;

import org.apache.coyote.http11.upgrade.servlet31.HttpUpgradeHandler;
import org.apache.tomcat.util.net.SSLSupport;
import org.apache.tomcat.util.net.SocketState;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.coyote.http11.upgrade.UpgradeInbound;

/**
 * Common interface for processors of all protocols.
 */
public interface Processor<S> {
    public Executor getExecutor();

    public SocketState process(SocketWrapper<S> socketWrapper) throws IOException;

    public SocketState event(SocketStatus status) throws IOException;

    public SocketState asyncDispatch(SocketStatus status);
    public SocketState asyncPostProcess();

    /**
     * @deprecated  Will be removed in Tomcat 8.0.x.
     */
    @Deprecated
    public UpgradeInbound getUpgradeInbound();
    /**
     * @deprecated  Will be removed in Tomcat 8.0.x.
     */
    @Deprecated
    public SocketState upgradeDispatch() throws IOException;

    public HttpUpgradeHandler getHttpUpgradeHandler();
    public SocketState upgradeDispatch(SocketStatus status) throws IOException;
    
    public void errorDispatch();

    public boolean isComet();
    public boolean isAsync();
    public boolean isUpgrade();

    public Request2 getRequest();

    public void recycle(boolean socketClosing);

    public void setSslSupport(SSLSupport sslSupport);
}
