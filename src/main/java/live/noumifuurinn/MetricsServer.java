package live.noumifuurinn;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import live.noumifuurinn.metrics.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.util.StringUtil;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.unixdomain.server.UnixDomainServerConnector;

import java.net.InetSocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class MetricsServer {
    private static final Map<ModConfigSpec.BooleanValue, Function<MeterRegistry, Metric>> MERTIC_MAP;

    static {
        MERTIC_MAP = new HashMap<>();

        MERTIC_MAP.put(Config.Meters.PROCESSOR, Processor::new);
        MERTIC_MAP.put(Config.Meters.GC, GarbageCollectorWrapper::new);
        MERTIC_MAP.put(Config.Meters.ENTITIES, Entities::new);
        MERTIC_MAP.put(Config.Meters.LOADED_CHUNKS, LoadedChunks::new);
        MERTIC_MAP.put(Config.Meters.MEMORY, Memory::new);
        MERTIC_MAP.put(Config.Meters.PLAYER_ONLINE, PlayerOnline::new);
        MERTIC_MAP.put(Config.Meters.PLAYERS_WORLD, WorldPlayers::new);
        MERTIC_MAP.put(Config.Meters.THREADS, ThreadsWrapper::new);
        MERTIC_MAP.put(Config.Meters.TICK_DURATION_AVERAGE, TickDurationAverageCollector::new);
        MERTIC_MAP.put(Config.Meters.TICK_DURATION_MAX, TickDurationMaxCollector::new);
        MERTIC_MAP.put(Config.Meters.TICK_DURATION_MEDIAN, TickDurationMedianCollector::new);
        MERTIC_MAP.put(Config.Meters.TICK_DURATION_MIN, TickDurationMinCollector::new);
        MERTIC_MAP.put(Config.Meters.TPS, Tps::new);
        MERTIC_MAP.put(Config.Meters.WORLD_SIZE, WorldSize::new);
    }

    private final CompositeMeterRegistry registry;
    private final NeoforgeExporter forgeExporter;

    private final Map<ModConfigSpec.BooleanValue, Metric> metrics = new HashMap<>();

    private Server server;

    public MetricsServer(CompositeMeterRegistry registry, NeoforgeExporter forgeExporter) {
        this.registry = registry;
        this.forgeExporter = forgeExporter;
    }

    public void start() {
        if (Config.Prometheus.ENABLED.get()) {
            startPrometheus();
        }

        reloadMeters();
    }

    @SubscribeEvent
    public void onConfigReload(ModConfigEvent.Reloading event) {
        if (!event.getConfig().getModId().equals(NeoforgeExporter.MODID)) {
            return;
        }

        reloadMeters();
    }

    private void reloadMeters() {
        MERTIC_MAP.forEach((metricConf, getter) -> {
            Metric metric = metrics.computeIfAbsent(metricConf, ignore -> getter.apply(registry));
            String metricPath = String.join(".", metricConf.getPath());
            if (metricConf.get()) {
                try {
                    metric.enable();
                    log.info("enable metric {}", metricPath);
                } catch (Exception e) {
                    log.warn("failed to enable metric {}", metricPath, e);
                }
            } else {
                try {
                    metric.disable();
                    log.info("disable metric {}", metricPath);
                } catch (Exception e) {
                    log.warn("failed to disable metric {}", metricPath, e);
                }
            }
        });
    }

    @SneakyThrows
    private void startPrometheus() {
        PrometheusMeterRegistry prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.add(prometheusMeterRegistry);

        GzipHandler handler = new GzipHandler();
        handler.setHandler(new MetricsController(forgeExporter, prometheusMeterRegistry));

        if (!StringUtil.isNullOrEmpty(Config.Prometheus.UNIX_SOCKET_PATH.get()) && isUnixSocketSupported()) {
            // 使用 Unix Socket
            server = new Server();
            UnixDomainServerConnector connector = new UnixDomainServerConnector(server);
            connector.setUnixDomainPath(Path.of(Config.Prometheus.UNIX_SOCKET_PATH.get()));
            // 可选：设置其他参数
            connector.setAcceptQueueSize(128);
            connector.setAcceptedReceiveBufferSize(8192);
            connector.setAcceptedSendBufferSize(8192);

            server.addConnector(connector);
            log.info("Started Prometheus metrics endpoint at: " + Config.Prometheus.UNIX_SOCKET_PATH.get());
        } else {
            // 使用 TCP Socket
            InetSocketAddress address = new InetSocketAddress(Config.Prometheus.HOST.get(), Config.Prometheus.PORT.get());
            server = new Server(address);
            log.info("Started Prometheus metrics endpoint at: " + Config.Prometheus.HOST.get() + ":" + Config.Prometheus.PORT.get());
        }
        server.setHandler(handler);

        server.start();
    }

    public void stop() throws Exception {
        if (server == null) {
            return;
        }
        server.stop();
    }

    private static boolean isUnixSocketSupported() {
        // 检查操作系统
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            return false;
        }

        // 尝试创建 Unix Socket 地址
        try {
            UnixDomainSocketAddress.of("/tmp/test.sock");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
