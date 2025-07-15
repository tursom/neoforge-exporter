package live.noumifuurinn;

import com.mojang.logging.LogUtils;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import live.noumifuurinn.utils.CommonUtils;
import lombok.SneakyThrows;
import net.minecraft.util.StringUtil;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(NeoforgeExporter.MODID)
public class NeoforgeExporter {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "neoforge_exporter";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final CompositeMeterRegistry registry = new CompositeMeterRegistry();
    private MetricsServer server;

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public NeoforgeExporter(IEventBus modEventBus, ModContainer modContainer) {
        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (NeoforgeExporter) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        if (!StringUtil.isNullOrEmpty(Config.prefix.get())) {
            registry.config().meterFilter(new MeterFilter() {
                @Override
                public Meter.@NotNull Id map(Meter.@NotNull Id id) {
                    return id.withName(Config.prefix.get() + id.getName());
                }
            });
        }
        if (!Config.tags.get().isEmpty()) {
            registry.config().commonTags(Config.tags.get().entrySet().stream()
                    .map((entry) -> Tag.of(entry.getKey(), entry.getValue()))
                    .toList());
        }

        CommonUtils.setServer(event.getServer());
        startMetricsServer();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // 服务器已完全停止时触发，用于清理资源
        LOGGER.info("Server stopping, shutting down metrics server...");

        if (server != null) {
            try {
                server.stop();
                LOGGER.info("Metrics server stopped successfully");
            } catch (Exception e) {
                LOGGER.warn("Failed to stop metrics server gracefully: " + e.getMessage(), e);
            }
        }

        // 清理服务器引用
        CommonUtils.setServer(null);
    }

    public Logger getLogger() {
        return LOGGER;
    }

    @SneakyThrows
    private void startMetricsServer() {
        server = new MetricsServer(registry, this);
        server.start();
    }
}
