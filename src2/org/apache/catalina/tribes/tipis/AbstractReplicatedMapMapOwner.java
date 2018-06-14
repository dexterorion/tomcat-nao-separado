package org.apache.catalina.tribes.tipis;

public interface AbstractReplicatedMapMapOwner {
    // a typo, should have been "objectMadePrimary"
    public void objectMadePrimay(Object key, Object value);
}