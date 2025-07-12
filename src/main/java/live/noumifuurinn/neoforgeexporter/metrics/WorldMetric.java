package live.noumifuurinn.neoforgeexporter.metrics;

import io.prometheus.client.Collector;
import live.noumifuurinn.neoforgeexporter.NeoforgeExporter;
import net.minecraft.server.level.ServerLevel;

public abstract class WorldMetric extends Metric {
    public WorldMetric(Collector collector) {
        super(collector);
    }

    @Override
    public final void doCollect() {
        clear();
        for (ServerLevel world : NeoforgeExporter.getServer().getAllLevels()) {
            collect(world);
        }
    }

    protected abstract void clear();
    protected abstract void collect(ServerLevel world);
/*
    protected String getEntityName(EntityType<> type) {
        try {
            return type.getKey().getKey();y
        } catch (Throwable t) {
            // Note: The entity type key above was introduced in 1.14. Older implementations should fallback here.
            return type.name();
        }
    }
    */
}
