package org.conf4j.consul;

import com.orbitz.consul.Consul;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

public class StandaloneConsul {

    private static final Logger logger = LoggerFactory.getLogger(StandaloneConsul.class);

    private final GenericContainer consulContainer;
    private Consul consul;

    public StandaloneConsul() {
        consulContainer = new GenericContainer("consul:0.9.0")
                .withCommand("agent -dev -client 0.0.0.0")
                .withExposedPorts(8500);
    }

    public void start() {
        consulContainer.start();
        consulContainer.followOutput(new Slf4jLogConsumer(logger).withPrefix("consul"));
        consul = Consul.builder()
                .withUrl(getConsulUrl())
                .build();
    }

    public void stop() {
        consul = null;
        consulContainer.stop();
    }

    public Consul getConsul() {
        if (consul == null) {
            throw new IllegalStateException("Standalone consul must be started before calling getConsul");
        }

        return consul;
    }

    public String getConsulUrl() {
        return String.format("http://localhost:%s", consulContainer.getMappedPort(8500));
    }

}
