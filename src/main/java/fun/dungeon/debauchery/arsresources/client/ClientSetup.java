package fun.dungeon.debauchery.arsresources.client;

import fun.dungeon.debauchery.arsresources.ArsReSources;
import fun.dungeon.debauchery.arsresources.ModRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ArsReSources.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModRegistry.SOURCE_VESSEL_TILE.get(), SourceVesselRenderer::new);
    }
}
