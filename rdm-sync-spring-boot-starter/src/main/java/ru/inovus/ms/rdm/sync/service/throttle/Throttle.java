package ru.inovus.ms.rdm.sync.service.throttle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Throttle {

    private final Logger logger = LoggerFactory.getLogger(Throttle.class);

    private final long delayMillis;
    private long prevRequestMillis;

    public Throttle(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    synchronized void throttleAndUpdateRequestTime() {
        if (prevRequestMillis == 0L) {
            prevRequestMillis = System.currentTimeMillis();
            return;
        }
        while (true) {
            long dt = System.currentTimeMillis() - prevRequestMillis;
            try {
                if (dt > 0 && dt < delayMillis)
                    wait(delayMillis - dt);
                else
                    break;
            } catch (InterruptedException e) {
                logger.warn("Throttling was interrupted. ", e);
                Thread.currentThread().interrupt();
            }
        }
        prevRequestMillis = System.currentTimeMillis();
    }

}
