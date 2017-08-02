package org.conf4j.core.source.reload;

import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static org.awaitility.Awaitility.await;

public class PeriodicalReloadStrategyTest {

    @Test
    public void testReloadCalledPeriodically() {
        LongAdder numOfReloadCalls = new LongAdder();
        PeriodicalReloadStrategy strategy = PeriodicalReloadStrategy.builder()
                .withInterval(Duration.ofMillis(50))
                .build();

        try {
            strategy.start(numOfReloadCalls::increment);

            await("Reload called more then once")
                    .atMost(5, TimeUnit.SECONDS)
                    .until(() -> numOfReloadCalls.longValue() > 1);
        } finally {
            strategy.stop();
        }
    }

    @Test
    public void testExceptionWhileCallingReloadCaught() {
        LongAdder numOfReloadCalls = new LongAdder();
        PeriodicalReloadStrategy strategy = PeriodicalReloadStrategy.builder()
                .withInterval(Duration.ofMillis(50))
                .build();

        try {
            strategy.start(() -> {
                numOfReloadCalls.increment();
                if (numOfReloadCalls.longValue() == 1) {
                    throw new RuntimeException();
                }
            });

            await("Reload called more then once")
                    .atMost(5, TimeUnit.SECONDS)
                    .until(() -> numOfReloadCalls.longValue() > 1);
        } finally {
            strategy.stop();
        }
    }
}