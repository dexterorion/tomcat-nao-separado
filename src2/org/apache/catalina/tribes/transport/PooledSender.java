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
package org.apache.catalina.tribes.transport;

import java.io.IOException;

import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.util.StringManager;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public abstract class PooledSender extends AbstractSender implements MultiPointSender {
    
    private static final Log log = LogFactory.getLog(PooledSender.class);
    private static final StringManager sm =
        StringManager.getManager(Constants20.getPackage());
    
    private PooledSenderSenderQueue queue = null;
    private int poolSize = 25;
    private long maxWait = 3000;
    public PooledSender() {
        queue = new PooledSenderSenderQueue(this,poolSize);
    }
    
    public abstract DataSender getNewDataSender();
    
    public DataSender getSender() {
        return queue.getSender(getMaxWait());
    }
    
    public void returnSender(DataSender sender) {
        sender.keepalive();
        queue.returnSender(sender);
    }
    
    @Override
    public synchronized void connect() throws IOException {
        //do nothing, happens in the socket sender itself
        queue.open();
        setConnected(true);
    }
    
    @Override
    public synchronized void disconnect() {
        queue.close();
        setConnected(false);
    }
    
    
    public int getInPoolSize() {
        return queue.getInPoolSize();
    }

    public int getInUsePoolSize() {
        return queue.getInUsePoolSize();
    }


    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
        queue.setLimit(poolSize);
    }

    public int getPoolSize() {
        return poolSize;
    }

    public long getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }

    @Override
    public boolean keepalive() {
        //do nothing, the pool checks on every return
        return (queue==null)?false:queue.checkIdleKeepAlive();
    }

    @Override
    public void add(Member member) {
        // no op, senders created upon demands
    }

    @Override
    public void remove(Member member) {
        //no op for now, should not cancel out any keys
        //can create serious sync issues
        //all TCP connections are cleared out through keepalive
        //and if remote node disappears
    }

	public static Log getLog() {
		return log;
	}

	public static StringManager getSm() {
		return sm;
	}
}