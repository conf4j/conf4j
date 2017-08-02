package org.conf4j.core.source.reload;

public interface ReloadStrategy {

    void start(Runnable reloadCallback);
    default void stop() {}

}
