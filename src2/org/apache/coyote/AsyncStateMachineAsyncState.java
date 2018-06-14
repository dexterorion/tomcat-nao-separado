package org.apache.coyote;

public enum AsyncStateMachineAsyncState {
    DISPATCHED(false, false, false),
    STARTING(true, true, false),
    STARTED(true, true, false),
    MUST_COMPLETE(true, false, false),
    COMPLETING(true, false, false),
    TIMING_OUT(true, false, false),
    MUST_DISPATCH(true, true, true),
    DISPATCHING(true, false, true),
    ERROR(true,false,false);

    private boolean isAsync;
    private boolean isStarted;
    private boolean isDispatching;
    
    private AsyncStateMachineAsyncState(boolean isAsync, boolean isStarted,
            boolean isDispatching) {
        this.isAsync = isAsync;
        this.isStarted = isStarted;
        this.isDispatching = isDispatching;
    }
    
    public boolean isAsync() {
        return this.isAsync;
    }
    
    public boolean isStarted() {
        return this.isStarted;
    }
    
    public boolean isDispatching() {
        return this.isDispatching;
    }
}