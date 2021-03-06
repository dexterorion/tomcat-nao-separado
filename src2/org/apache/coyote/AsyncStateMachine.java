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

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.tomcat.util.net.SocketState;
import org.apache.tomcat.util.res.StringManager3;
import org.apache.tomcat.util.security.PrivilegedGetTccl;
import org.apache.tomcat.util.security.PrivilegedSetTccl;

/**
 * Manages the state transitions for async requests.
 * 
 * <pre>
 * The internal states that are used are:
 * DISPATCHED    - Standard request. Not in Async mode.
 * STARTING      - ServletRequest.startAsync() has been called but the
 *                 request in which that call was made has not finished
 *                 processing.
 * STARTED       - ServletRequest.startAsync() has been called and the
 *                 request in which that call was made has finished
 *                 processing.
 * MUST_COMPLETE - complete() has been called before the request in which
 *                 ServletRequest.startAsync() has finished. As soon as that
 *                 request finishes, the complete() will be processed.
 * COMPLETING    - The call to complete() was made once the request was in
 *                 the STARTED state. May or may not be triggered by a
 *                 container thread - depends if start(Runnable) was used
 * TIMING_OUT    - The async request has timed out and is waiting for a call
 *                 to complete(). If that isn't made, the error state will
 *                 entered.
 * MUST_DISPATCH - dispatch() has been called before the request in which
 *                 ServletRequest.startAsync() has finished. As soon as that
 *                 request finishes, the dispatch() will be processed.
 * DISPATCHING   - The dispatch is being processed.
 * ERROR         - Something went wrong.
 *
 * |----------------->--------------|
 * |                               \|/
 * |   |----------<---------------ERROR
 * |   |      complete()         /|\ | \                                                          
 * |   |                          |  |  \---------------|                                         
 * |   |                          |  |                  |dispatch()                               
 * |   |                          |  |postProcess()    \|/                                        
 * |   |                   error()|  |                  |                                         
 * |   |                          |  |  |--|timeout()   |                                         
 * |   |           postProcess()  | \|/ | \|/           |         auto                            
 * |   |         |--------------->DISPATCHED<---------- | --------------COMPLETING<-----|         
 * |   |         |               /|\  |                 |                 | /|\         |         
 * |   |         |    |--->-------|   |                 |                 |--|          |         
 * |   |         ^    |               |startAsync()     |               timeout()       |         
 * |   |         |    |               |                 |                               |         
 * |  \|/        |    |  complete()  \|/  postProcess() |                               |         
 * | MUST_COMPLETE-<- | ----<------STARTING-->--------- | ------------|                 ^         
 * |         /|\      |               |                 |             |      complete() |         
 * |          |       |               |                 |             |     /-----------|         
 * |          |       ^               |dispatch()       |             |    /                      
 * |          |       |               |                 |             |   /                       
 * |          |       |              \|/                /            \|/ /                        
 * |          |       |         MUST_DISPATCH          /           STARTED
 * |          |       |           |                   /            /|  
 * |          |       |           |postProcess()     /            / |   
 * ^          ^       |           |                 /  dispatch()/  |    
 * |          |       |           |                /            /   |    
 * |          |       |           |   |---------- / -----------/    |auto
 * |          |       |           |   |          /                  |   
 * |          |       |           |   |   |-----/                   |   
 * |          |       | auto     \|/ \|/ \|/                       \|/  
 * |          |       |---<------DISPATCHING<-----------------TIMING_OUT
 * |          |                               dispatch()        |   |
 * |          |                                                 |   |
 * |          |-------<----------------------------------<------|   |
 * |                              complete()                        |  
 * |                                                                |  
 * |<--------<-------------------<-------------------------------<--|  
 *                                 error()                             
 * </pre>
 */
public class AsyncStateMachine<S> {

    /**
     * The string manager for this package.
     */
    private static final StringManager3 sm =
        StringManager3.getManager(Constants24.getPackage());

    private volatile AsyncStateMachineAsyncState state = AsyncStateMachineAsyncState.DISPATCHED;
    // Need this to fire listener on complete
    private AsyncContextCallback asyncCtxt = null;
    private Processor<S> processor;
    
    
    public AsyncStateMachine(Processor<S> processor) {
        this.processor = processor;
    }


    public boolean isAsync() {
        return state.isAsync();
    }

    public boolean isAsyncDispatching() {
        return state.isDispatching();
    }

    public boolean isAsyncStarted() {
        return state.isStarted();
    }

    public boolean isAsyncTimingOut() {
        return state == AsyncStateMachineAsyncState.TIMING_OUT;
    }

    public boolean isAsyncError() {
        return state == AsyncStateMachineAsyncState.ERROR;
    }

    public synchronized void asyncStart(AsyncContextCallback asyncCtxt) {
        if (state == AsyncStateMachineAsyncState.DISPATCHED) {
            state = AsyncStateMachineAsyncState.STARTING;
            this.asyncCtxt = asyncCtxt;
        } else {
            throw new IllegalStateException(
                    sm.getString("asyncStateMachine.invalidAsyncState",
                            "asyncStart()", state));
        }
    }
    
    /*
     * Async has been processed. Whether or not to enter a long poll depends on
     * current state. For example, as per SRV.2.3.3.3 can now process calls to
     * complete() or dispatch().
     */
    public synchronized SocketState asyncPostProcess() {
        
        if (state == AsyncStateMachineAsyncState.STARTING) {
            state = AsyncStateMachineAsyncState.STARTED;
            return SocketState.LONG;
        } else if (state == AsyncStateMachineAsyncState.MUST_COMPLETE) {
            asyncCtxt.fireOnComplete();
            state = AsyncStateMachineAsyncState.DISPATCHED;
            return SocketState.ASYNC_END;
        } else if (state == AsyncStateMachineAsyncState.COMPLETING) {
            asyncCtxt.fireOnComplete();
            state = AsyncStateMachineAsyncState.DISPATCHED;
            return SocketState.ASYNC_END;
        } else if (state == AsyncStateMachineAsyncState.MUST_DISPATCH) {
            state = AsyncStateMachineAsyncState.DISPATCHING;
            return SocketState.ASYNC_END;
        } else if (state == AsyncStateMachineAsyncState.DISPATCHING) {
            state = AsyncStateMachineAsyncState.DISPATCHED;
            return SocketState.ASYNC_END;
        } else if (state == AsyncStateMachineAsyncState.STARTED) {
            // This can occur if an async listener does a dispatch to an async
            // servlet during onTimeout
            return SocketState.LONG;
        } else {
            throw new IllegalStateException(
                    sm.getString("asyncStateMachine.invalidAsyncState",
                            "asyncPostProcess()", state));
        }
    }
    

    public synchronized boolean asyncComplete() {
        boolean doComplete = false;
        
        if (state == AsyncStateMachineAsyncState.STARTING) {
            state = AsyncStateMachineAsyncState.MUST_COMPLETE;
        } else if (state == AsyncStateMachineAsyncState.STARTED) {
            state = AsyncStateMachineAsyncState.COMPLETING;
            doComplete = true;
        } else if (state == AsyncStateMachineAsyncState.TIMING_OUT ||
                state == AsyncStateMachineAsyncState.ERROR) {
            state = AsyncStateMachineAsyncState.MUST_COMPLETE;
        } else {
            throw new IllegalStateException(
                    sm.getString("asyncStateMachine.invalidAsyncState",
                            "asyncComplete()", state));
            
        }
        return doComplete;
    }
    
    
    public synchronized boolean asyncTimeout() {
        if (state == AsyncStateMachineAsyncState.STARTED) {
            state = AsyncStateMachineAsyncState.TIMING_OUT;
            return true;
        } else if (state == AsyncStateMachineAsyncState.COMPLETING ||
                state == AsyncStateMachineAsyncState.DISPATCHING ||
                state == AsyncStateMachineAsyncState.DISPATCHED) {
            // NOOP - App called complete() or dispatch() between the the
            // timeout firing and execution reaching this point
            return false;
        } else {
            throw new IllegalStateException(
                    sm.getString("asyncStateMachine.invalidAsyncState",
                            "asyncTimeout()", state));
        }
    }
    
    
    public synchronized boolean asyncDispatch() {
        boolean doDispatch = false;
        if (state == AsyncStateMachineAsyncState.STARTING) {
            state = AsyncStateMachineAsyncState.MUST_DISPATCH;
        } else if (state == AsyncStateMachineAsyncState.STARTED ||
                state == AsyncStateMachineAsyncState.TIMING_OUT ||
                state == AsyncStateMachineAsyncState.ERROR) {
            state = AsyncStateMachineAsyncState.DISPATCHING;
            doDispatch = true;
        } else {
            throw new IllegalStateException(
                    sm.getString("asyncStateMachine.invalidAsyncState",
                            "asyncDispatch()", state));
        }
        return doDispatch;
    }
    
    
    public synchronized void asyncDispatched() {
        if (state == AsyncStateMachineAsyncState.DISPATCHING) {
            state = AsyncStateMachineAsyncState.DISPATCHED;
        } else {
            throw new IllegalStateException(
                    sm.getString("asyncStateMachine.invalidAsyncState",
                            "asyncDispatched()", state));
        }
    }
    
    
    public synchronized boolean asyncError() {
        boolean doDispatch = false;
        if (state == AsyncStateMachineAsyncState.DISPATCHED ||
                state == AsyncStateMachineAsyncState.TIMING_OUT) {
            state = AsyncStateMachineAsyncState.ERROR;
        } else {
            throw new IllegalStateException(
                    sm.getString("asyncStateMachine.invalidAsyncState",
                            "asyncError()", state));
        }
        return doDispatch;
    }
    
    public synchronized void asyncRun(Runnable runnable) {
        if (state == AsyncStateMachineAsyncState.STARTING || state ==  AsyncStateMachineAsyncState.STARTED) {
            // Execute the runnable using a container thread from the
            // Connector's thread pool. Use a wrapper to prevent a memory leak
            ClassLoader oldCL;
            if (Constants24.isSecurityEnabled()) {
                PrivilegedAction<ClassLoader> pa = new PrivilegedGetTccl();
                oldCL = AccessController.doPrivileged(pa);
            } else {
                oldCL = Thread.currentThread().getContextClassLoader();
            }
            try {
                if (Constants24.isSecurityEnabled()) {
                    PrivilegedAction<Void> pa = new PrivilegedSetTccl(
                            this.getClass().getClassLoader());
                    AccessController.doPrivileged(pa);
                } else {
                    Thread.currentThread().setContextClassLoader(
                            this.getClass().getClassLoader());
                }
                
                processor.getExecutor().execute(runnable);
            } finally {
                if (Constants24.isSecurityEnabled()) {
                    PrivilegedAction<Void> pa = new PrivilegedSetTccl(
                            oldCL);
                    AccessController.doPrivileged(pa);
                } else {
                    Thread.currentThread().setContextClassLoader(oldCL);
                }
            }
        } else {
            throw new IllegalStateException(
                    sm.getString("asyncStateMachine.invalidAsyncState",
                            "asyncRun()", state));
        }

    }
    
    
    public synchronized void recycle() {
        asyncCtxt = null;
        state = AsyncStateMachineAsyncState.DISPATCHED;
    }
}
