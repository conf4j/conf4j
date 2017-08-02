package org.conf4j.core.source.reload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

import static java.util.Objects.requireNonNull;

public class PeriodicalReloadStrategy implements ReloadStrategy {

    private final Logger logger = LoggerFactory.getLogger(PeriodicalReloadStrategy.class);

    private final Duration interval;
    private Timer reloadTimer;
    private TimerTask reloadTask;

    private PeriodicalReloadStrategy(Duration interval) {
        this.interval = requireNonNull(interval);
    }

    @Override
    public void start(Runnable reloadCallback) {
        logger.info("Starting periodical reload strategy, reload interval set to: {}ms", interval.toMillis());
        reloadTimer = new Timer(true);
        reloadTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    logger.trace("Triggering configuration reload");
                    reloadCallback.run();
                } catch (Throwable t) {
                    logger.error("Unknown error thrown while reloading config", t);
                }
            }
        };

        reloadTimer.scheduleAtFixedRate(reloadTask, interval.toMillis(), interval.toMillis());
        logger.info("Periodical reload strategy started");
    }

    @Override
    public void stop() {
        logger.info("Stopping periodical reload strategy");
        if (reloadTask != null) reloadTask.cancel();
        if (reloadTimer != null) reloadTimer.cancel();
        logger.info("Periodical reload strategy stopped");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Duration interval;

        private Builder() {}

        public Builder withInterval(Duration interval) {
            this.interval = interval;
            return this;
        }

        public PeriodicalReloadStrategy build() {
            return new PeriodicalReloadStrategy(interval);
        }

    }

}
