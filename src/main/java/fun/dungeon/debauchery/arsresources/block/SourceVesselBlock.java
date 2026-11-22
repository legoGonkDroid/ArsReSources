package fun.dungeon.debauchery.arsresources.block;

import com.hollingsworth.arsnouveau.common.block.ModBlock;
import com.hollingsworth.arsnouveau.common.block.TickableModBlock;
import com.hollingsworth.arsnouveau.common.items.DominionWand;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import fun.dungeon.debauchery.arsresources.config.VesselCosts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Source Vessel. Right-click with an item to set what it copies, right-click with an empty hand to take it back.
 * Ticking is provided by Ars Nouveau's {@link TickableModBlock}.
 */
public class SourceVesselBlock extends TickableModBlock {
    // Matches models/block/source_vessel.json: plinth, stem, bowl and the four corner prongs.
    private static final VoxelShape SHAPE = Shapes.join(
            Shapes.or(
                    Block.box(2, 0, 2, 14, 3, 14),
                    Block.box(6, 3, 6, 10, 6, 10),
                    Block.box(3, 6, 3, 13, 11, 13),
                    Block.box(3, 11, 3, 4, 14, 4),
                    Block.box(12, 11, 3, 13, 14, 4),
                    Block.box(3, 11, 12, 4, 14, 13),
                    Block.box(12, 11, 12, 13, 14, 13)),
            Block.box(4, 8, 4, 12, 11, 12),
            BooleanOp.ONLY_FIRST);

    public SourceVesselBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties defaultVesselProperties() {
        return ModBlock.defaultProperties().noOcclusion().sound(SoundType.STONE).lightLevel(state -> 6);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SourceVesselTile(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        // useItemOn also runs with an empty hand: pass that to useWithoutItem, which hands the template back.
        // Let the Dominion Wand link inventories instead of being inserted as a template.
        if (held.isEmpty() || held.getItem() instanceof DominionWand || hand != InteractionHand.MAIN_HAND) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof SourceVesselTile vessel)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (VesselCosts.costFor(held) <= 0) {
            PortUtil.sendMessageNoSpam(player, Component.translatable("resources.vessel.not_copyable", held.getHoverName()));
            return ItemInteractionResult.CONSUME;
        }
        ItemStack previous = vessel.getTemplate();
        vessel.setTemplate(held.split(1));
        if (!previous.isEmpty()) {
            giveOrDrop(player, previous);
        }
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof SourceVesselTile vessel)
                || vessel.getTemplate().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            giveOrDrop(player, vessel.removeItemNoUpdate(0));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SourceVesselTile vessel) {
            Containers.dropContents(level, pos, vessel);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
