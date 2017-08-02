package org.conf4j.consul.source;

import com.orbitz.consul.KeyValueClient;

import java.time.Duration;

public interface ConsulConfigurationSource {

    KeyValueClient getKeyValueClient();
    String getPathToWatch();
    Duration getReadTimeout();

}
