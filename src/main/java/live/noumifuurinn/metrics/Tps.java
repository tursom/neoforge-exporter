package live.noumifuurinn.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import live.noumifuurinn.NeoforgeExporter;
import live.noumifuurinn.tps.TpsCollector;

import java.util.Collection;
import java.util.List;

public class Tps extends Metric {
    private final TpsCollector tpsCollector = new TpsCollector();

    public Tps(MeterRegistry registry) {
        super(registry);
    }

    @Override
    public void enable() {
        super.enable();
        NeoforgeExporter.registerServerTickEvent(this, tpsCollector);
    }

    @Override
    public void disable() {
        super.disable();
        NeoforgeExporter.unregisterServerTickEvent(this);
    }

    @Override
    public Collection<Meter> register() {
        return List.of(Gauge.builder(prefix("tps"), tpsCollector, TpsCollector::getAverageTPS)
                .description("Server TPS (ticks per second)")
                .register(registry));
    }
}
