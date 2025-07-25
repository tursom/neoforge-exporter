package live.noumifuurinn.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Collection;
import java.util.List;

public class TickDurationMaxCollector extends TickDurationCollector {
    public TickDurationMaxCollector(MeterRegistry registry) {
        super(registry);
    }

    private long getTickDurationMax() {
        long max = Long.MIN_VALUE;
        for (Long val : getTickDurations()) {
            if (val > max) {
                max = val;
            }
        }
        return max;
    }

    @Override
    public Collection<Meter> register() {
        return List.of(Gauge.builder("tick.duration.max", this, TickDurationMaxCollector::getTickDurationMax)
                .strongReference(true)
                .register(registry));
    }
}

