package live.noumifuurinn.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import live.noumifuurinn.NeoforgeExporter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PlayerOnline extends Metric {
    private final ConcurrentMap<UUID, PlayerStatus> status = new ConcurrentHashMap<>();

    public PlayerOnline(MeterRegistry registry) {
        super(registry);

        // 注册到Forge事件总线
        NeoForge.EVENT_BUS.register(this);
    }

    @Override
    public Set<Meter> register() {
        var meters = new HashSet<Meter>();
        for (ServerPlayer player : NeoforgeExporter.getServer().getPlayerList().getPlayers()) {
            meters.add(register(player));
        }
        return meters;
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        meters.add(register(event.getEntity()));
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        remove(event.getEntity());
    }

    private Meter register(Player player) {
        PlayerStatus playerStatus = status.computeIfAbsent(
                player.getUUID(),
                ignore -> {
                    PlayerStatus ps = new PlayerStatus();
                    ps.gauge = Gauge.builder(prefix("player.online"), ps, PlayerStatus::getState)
                            .description("Online state by player name")
                            .tag("name", player.getName().getString())
                            .tag("uid", player.getUUID().toString())
                            .register(registry);
                    return ps;
                }
        );
        playerStatus.state = 1;
        return playerStatus.gauge;
    }

    private void remove(Player player) {
        UUID uuid = player.getUUID();
        PlayerStatus playerStatus = status.get(uuid);
        if (playerStatus == null || playerStatus.state == 0) {
            return;
        }

        playerStatus.state = 0;
        Thread.ofVirtual().start(() -> remove(uuid, playerStatus));
    }

    @SneakyThrows
    private void remove(UUID uuid, PlayerStatus playerStatus) {
        Gauge gauge = playerStatus.gauge;
        if (gauge == null) {
            return;
        }

        // 等待5分钟后删除指标
        Thread.sleep(5 * 60_000);
        if (status.remove(uuid, new PlayerStatus(0))) {
            registry.remove(gauge);
            meters.remove(gauge);
        }
    }

    @Data
    @NoArgsConstructor
    private static class PlayerStatus {
        private double state;
        @EqualsAndHashCode.Exclude
        private Gauge gauge;

        public PlayerStatus(double state) {
            this.state = state;
        }
    }
}
