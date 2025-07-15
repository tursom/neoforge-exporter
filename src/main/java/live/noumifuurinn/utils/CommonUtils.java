package live.noumifuurinn.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 抽取出的与 mod 端有关的代码，方便不同 mod 端之间的移植
 */
@UtilityClass
@Slf4j
public class CommonUtils {
    private final Map<Object, Runnable> serverTickReg = new ConcurrentHashMap<>();
    private final EventHandler eventHandler = new EventHandler();

    @Getter
    @Setter
    private static MinecraftServer server;

    public long[] getTickTimesNanos() {
        return server.getTickTimesNanos();
    }

    public void onPlayerJoin(Consumer<Player> consumer) {
        eventHandler.playerJoinHandlers.add(consumer);
    }

    public void onPlayerLeave(Consumer<Player> consumer) {
        eventHandler.playerLeaveHandlers.add(consumer);
    }

    public void executeAfter(long delay, TimeUnit timeUnit, Runnable task) {
        Thread.ofVirtual().start(() -> {
            delay(delay, timeUnit);
            task.run();
        });
    }

    public void registerServerTickEvent(Object parent, Runnable r) {
        serverTickReg.put(parent, r);
    }

    public void unregisterServerTickEvent(Object parent) {
        serverTickReg.remove(parent);
    }

    @SneakyThrows
    private void delay(long delay, TimeUnit timeUnit) {
        Thread.sleep(timeUnit.toMillis(delay));
    }

    private static class EventHandler {
        private final List<Consumer<Player>> playerJoinHandlers = new ArrayList<>();
        private final List<Consumer<Player>> playerLeaveHandlers = new ArrayList<>();

        {
            NeoForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void onTick(ServerTickEvent.Pre event) {
            for (Runnable r : serverTickReg.values()) {
                try {
                    r.run();
                } catch (Throwable t) {
                    log.warn("Error in server tick event", t);
                }
            }
        }

        @SubscribeEvent
        public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            for (Consumer<Player> consumer : playerJoinHandlers) {
                try {
                    consumer.accept(event.getEntity());
                } catch (Exception e) {
                    log.warn("Error in player join event handler", e);
                }
            }
        }

        @SubscribeEvent
        public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
            for (Consumer<Player> consumer : playerLeaveHandlers) {
                try {
                    consumer.accept(event.getEntity());
                } catch (Exception e) {
                    log.warn("Error in player leave event handler", e);
                }
            }
        }
    }
}
