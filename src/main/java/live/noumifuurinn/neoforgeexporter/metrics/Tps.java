package live.noumifuurinn.neoforgeexporter.metrics;

import io.prometheus.client.Gauge;
import live.noumifuurinn.neoforgeexporter.NeoforgeExporter;
import live.noumifuurinn.neoforgeexporter.tps.TpsCollector;

public class Tps extends Metric {

    private static final Gauge TPS = Gauge.build()
            .name(prefix("tps"))
            .help("Server TPS (ticks per second)")
            .create();

    private TpsCollector tpsCollector = new TpsCollector();

    public Tps() {
        super(TPS);
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
    public void doCollect() {
        TPS.set(tpsCollector.getAverageTPS());
    }
}
