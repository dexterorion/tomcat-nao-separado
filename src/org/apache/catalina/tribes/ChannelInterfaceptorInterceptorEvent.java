package org.apache.catalina.tribes;

public interface ChannelInterfaceptorInterceptorEvent {
    public int getEventType();
    public String getEventTypeDesc();
    public ChannelInterceptor getInterceptor();
}