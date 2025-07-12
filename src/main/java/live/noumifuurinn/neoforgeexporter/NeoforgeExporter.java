package live.noumifuurinn.neoforgeexporter;

import com.mojang.logging.LogUtils;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.Map;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(NeoforgeExporter.MODID)
public class NeoforgeExporter {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "neoforge_exporter";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    private final static Map<Object, Runnable> serverTickReg = new java.util.concurrent.ConcurrentHashMap<>();
    private static MinecraftServer mcServer;

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
        mcServer = event.getServer();

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
        mcServer = null;
    }

    @SubscribeEvent
    public void onTick(ServerTickEvent.Pre event) {
        for (Runnable r : serverTickReg.values()) {
            try {
                r.run();
            } catch (Throwable t) {

            }
        }
    }

    public static void registerServerTickEvent(Object parent, Runnable r) {
        serverTickReg.put(parent, r);
    }

    public static void unregisterServerTickEvent(Object parent) {
        serverTickReg.remove(parent);
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public static MinecraftServer getServer() {
        return mcServer;
    }

    @SneakyThrows
    private void startMetricsServer() {
        String host = Config.Host.get();
        Integer port = Config.Port.get();
        String unixSocketPath = Config.UNIX_SOCKET_PATH.get();

        server = new MetricsServer(host, port, unixSocketPath, this);
        server.start();
    }
}
