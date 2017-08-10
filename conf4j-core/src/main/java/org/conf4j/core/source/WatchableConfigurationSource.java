package org.conf4j.core.source;

import org.conf4j.core.source.reload.ReloadStrategy;

public interface WatchableConfigurationSource extends ConfigurationSource {

    boolean shouldWatchForChange();
    ReloadStrategy getReloadStrategy();

}
