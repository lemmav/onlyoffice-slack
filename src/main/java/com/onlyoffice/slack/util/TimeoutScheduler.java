package com.onlyoffice.slack.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TimeoutScheduler {
    private final ScheduledExecutorService pool = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    public void setTimeout(Runnable runnable, TimeUnit unit, int delay) throws RuntimeException {
        log.debug("scheduling a new task");
        pool.schedule(runnable, delay, unit);
    }
}
