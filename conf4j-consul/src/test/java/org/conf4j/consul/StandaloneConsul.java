package org.conf4j.consul;

import com.orbitz.consul.Consul;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;

public class StandaloneConsul {

    private static final Logger logger = LoggerFactory.getLogger(StandaloneConsul.class);

    private final GenericContainer consulContainer;
    private AtomicReference<Consul> consul = new AtomicReference<>();

    public StandaloneConsul() {
        consulContainer = new GenericContainer("consul:0.9.0")
                .withCommand("agent -dev -client 0.0.0.0")
                .withExposedPorts(8500);
    }

    public void start() {
        consulContainer.start();
        consulContainer.followOutput(new Slf4jLogConsumer(logger).withPrefix("consul"));
        await("Consul is ready")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        consul.set(Consul.builder().withUrl(getConsulUrl()).build());
                        return true;
                    } catch (Exception e) {
                        logger.info("Failed to create consul client, probably consul is not ready yet", e);
                        return false;
                    }
                });
    }

    public void stop() {
        consul = null;
        consulContainer.stop();
    }

    public Consul getConsul() {
        if (consul.get() == null) {
            throw new IllegalStateException("Standalone consul must be started before calling getConsul");
        }

        return consul.get();
    }

    public String getConsulUrl() {
        return String.format("http://localhost:%s", consulContainer.getMappedPort(8500));
    }

}
