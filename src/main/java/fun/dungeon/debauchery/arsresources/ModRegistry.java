package fun.dungeon.debauchery.arsresources;

import fun.dungeon.debauchery.arsresources.block.SourceVesselBlock;
import fun.dungeon.debauchery.arsresources.block.SourceVesselTile;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistry {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ArsReSources.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArsReSources.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ArsReSources.MODID);

    /** The block id is also Ars Deco's dye target id: keep it stable (see ArsDecoTargetsMixin). */
    public static final String SOURCE_VESSEL_ID = "source_vessel";

    public static final DeferredBlock<SourceVesselBlock> SOURCE_VESSEL =
            BLOCKS.register(SOURCE_VESSEL_ID, () -> new SourceVesselBlock(SourceVesselBlock.defaultVesselProperties()));

    public static final DeferredItem<BlockItem> SOURCE_VESSEL_ITEM =
            ITEMS.registerSimpleBlockItem(SOURCE_VESSEL_ID, SOURCE_VESSEL);

    @SuppressWarnings("DataFlowIssue") // the data fixer type is unused for modded block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SourceVesselTile>> SOURCE_VESSEL_TILE =
            BLOCK_ENTITIES.register(SOURCE_VESSEL_ID,
                    () -> BlockEntityType.Builder.of(SourceVesselTile::new, SOURCE_VESSEL.get()).build(null));

    private ModRegistry() {
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
