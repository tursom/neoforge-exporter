package live.noumifuurinn.neoforgeexporter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = NeoforgeExporter.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static ModConfigSpec.ConfigValue<String> Host = BUILDER
            .comment("地址")
            .define("Host", "0.0.0.0");
    public static ModConfigSpec.IntValue Port = BUILDER
            .comment("端口")
            .defineInRange("Port", 9225, 1, 65535);
    public static ModConfigSpec.ConfigValue<String> UNIX_SOCKET_PATH = BUILDER
            .comment("Unix Socket路径")
            .define("unixSocketPath", "");

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
    }
}
