package fun.dungeon.debauchery.arsresources;

import com.hollingsworth.arsnouveau.setup.registry.CreativeTabRegistry;
import fun.dungeon.debauchery.arsresources.config.ResourcesConfig;
import fun.dungeon.debauchery.arsresources.config.VesselCosts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(ArsReSources.MODID)
public final class ArsReSources {
    public static final String MODID = "resources";

    public ArsReSources(IEventBus modBus, ModContainer container) {
        ModRegistry.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, ResourcesConfig.SPEC);
        modBus.addListener(ArsReSources::onConfig);
        modBus.addListener(ArsReSources::onConfigReload);
        modBus.addListener(ArsReSources::addToCreativeTabs);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private static void onConfig(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ResourcesConfig.SPEC) {
            VesselCosts.reload();
        }
    }

    private static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ResourcesConfig.SPEC) {
            VesselCosts.reload();
        }
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeTabRegistry.BLOCKS.getKey() || event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModRegistry.SOURCE_VESSEL_ITEM.get());
        }
    }
}
