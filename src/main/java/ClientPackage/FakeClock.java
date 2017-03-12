package ClientPackage;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by bingoc on 2017/2/23.
 */
public class FakeClock extends Clock {

    private AtomicLong millis = new AtomicLong(System.currentTimeMillis());
    private ZoneId zoneId = Clock.systemUTC().getZone();
    private boolean stopped;

    @Override
    public ZoneId getZone() {
        return zoneId;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        FakeClock clock = new FakeClock();
        clock.millis.set(this.millis.get());
        clock.zoneId = zone;
        return clock;
    }

    @Override
    public long millis() {
        if (stopped) {
            return millis.get();
        } else {
            return System.currentTimeMillis();
        }
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(millis());
    }

    public long incrementTimeMillis(long offsetMillis)
    {
        return this.millis.addAndGet(offsetMillis);
    }

    public long incrementTime(long time, TimeUnit timeUnit)
    {
        return incrementTimeMillis(timeUnit.toMillis(time));
    }


    public void stop()
    {
        stopped = true;
        millis.set(Math.max(System.currentTimeMillis(), millis.get()));
    }
}
