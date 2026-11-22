package fun.dungeon.debauchery.arsresources.block;

import com.hollingsworth.arsnouveau.api.client.ITooltipProvider;
import com.hollingsworth.arsnouveau.api.item.IWandable;
import com.hollingsworth.arsnouveau.api.source.AbstractSourceMachine;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.client.particle.ColorPos;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.common.block.ITickable;
import com.hollingsworth.arsnouveau.common.block.tile.CreativeSourceJarTile;
import com.hollingsworth.arsnouveau.common.capability.SourceStorage;
import com.hollingsworth.arsnouveau.common.entity.EntityFlyingItem;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import fun.dungeon.debauchery.arsresources.ModRegistry;
import fun.dungeon.debauchery.arsresources.compat.ArsDecoCompat;
import fun.dungeon.debauchery.arsresources.config.ResourcesConfig;
import fun.dungeon.debauchery.arsresources.config.VesselCosts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds one template item, draws source from nearby source jars (via Ars Nouveau's {@link SourceUtil}) and,
 * each time it has stored the configured cost, sends a copy of the template into a Dominion-Wand-linked
 * inventory with Ars Nouveau's {@link EntityFlyingItem} animation.
 *
 * <p>The vessel is deliberately not registered as a source capability, so relays and other machines
 * neither push into nor drain from it.
 */
@SuppressWarnings("removal") // ITooltipProvider is deprecated, but it is still what Ars Nouveau's look-at HUD reads
public class SourceVesselTile extends AbstractSourceMachine implements ITickable, IWandable, ITooltipProvider, Container {
    /** Height of the floating template above the block's bottom, in blocks. Shared with the renderer. */
    public static final double ITEM_HEIGHT = 0.78;

    private static final String TEMPLATE_TAG = "template";
    private static final String LINKS_TAG = "links";
    private static final String STATUS_TAG = "status";

    public enum Status {
        EMPTY, NOT_CONFIGURED, NO_LINK, OUTPUT_FULL, NO_SOURCE, WORKING;

        boolean isPaused() {
            return this != EMPTY && this != WORKING;
        }
    }

    private ItemStack template = ItemStack.EMPTY;
    private final List<BlockPos> linkedInvs = new ArrayList<>();
    private Status status = Status.EMPTY;
    private int tickCounter;

    public SourceVesselTile(BlockPos pos, BlockState state) {
        super(ModRegistry.SOURCE_VESSEL_TILE.get(), pos, state);
    }

    @Override
    protected @NotNull SourceStorage createDefaultStorage() {
        // Capacity only needs to cover the most expensive configured copy; the tick logic never pulls past the cost.
        return new SourceStorage(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 0) {
            @Override
            public void onContentsChanged() {
                SourceVesselTile.this.updateBlock();
            }
        };
    }

    // ---------------------------------------------------------------- ticking

    @Override
    public void tick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (++tickCounter < ResourcesConfig.PULL_INTERVAL_TICKS.get()) {
            return;
        }
        tickCounter = 0;
        setStatus(work(serverLevel));
    }

    private Status work(ServerLevel serverLevel) {
        if (template.isEmpty()) {
            return Status.EMPTY;
        }
        int cost = VesselCosts.costFor(template);
        if (cost <= 0) {
            return Status.NOT_CONFIGURED;
        }
        if (linkedInvs.isEmpty()) {
            return Status.NO_LINK;
        }
        ItemStack copy = template.copyWithCount(1);
        BlockPos output = findOutput(copy);
        if (output == null) {
            return Status.OUTPUT_FULL;
        }
        if (getSource() < cost) {
            int wanted = Math.min(ResourcesConfig.SOURCE_PER_PULL.get(), cost - getSource());
            int taken = pullSource(serverLevel, wanted);
            if (taken <= 0) {
                return Status.NO_SOURCE;
            }
            setSource(getSource() + taken);
        }
        if (getSource() >= cost) {
            produce(serverLevel, output, copy, cost);
        }
        return Status.WORKING;
    }

    /**
     * Takes up to {@code wanted} source from nearby jars. {@link SourceUtil#takeSourceMultipleWithParticles}
     * is all-or-nothing, so first cap the request at what the jars actually hold.
     */
    private int pullSource(ServerLevel serverLevel, int wanted) {
        int range = ResourcesConfig.JAR_RANGE.get();
        long available = 0;
        for (ISpecialSourceProvider provider : SourceUtil.canTakeSource(worldPosition, serverLevel, range)) {
            if (provider.getSource() instanceof CreativeSourceJarTile) {
                available = wanted;
                break;
            }
            available += provider.getSource().getSource();
            if (available >= wanted) {
                break;
            }
        }
        int request = (int) Math.min(wanted, available);
        if (request <= 0) {
            return 0;
        }
        return SourceUtil.takeSourceMultipleWithParticles(worldPosition, serverLevel, range, request) != null ? request : 0;
    }

    private void produce(ServerLevel serverLevel, BlockPos output, ItemStack copy, int cost) {
        IItemHandler handler = handlerAt(output);
        if (handler == null) {
            return;
        }
        ItemStack leftover = ItemHandlerHelper.insertItemStacked(handler, copy.copy(), false);
        if (!leftover.isEmpty()) {
            return; // the inventory changed since the simulation; try again next pull
        }
        setSource(getSource() - cost);

        // The same flying-item animation the Scribe's Table uses to pull items off pedestals, reversed.
        ParticleColor color = particleColor();
        Vec3 from = Vec3.atBottomCenterOf(worldPosition).add(0, ITEM_HEIGHT, 0);
        Vec3 to = Vec3.atCenterOf(output);
        EntityFlyingItem flying = new EntityFlyingItem(serverLevel, from, to,
                color.getRedInt(), color.getGreenInt(), color.getBlueInt());
        flying.setStack(copy);
        serverLevel.addFreshEntity(flying);
    }

    private @Nullable BlockPos findOutput(ItemStack copy) {
        for (BlockPos pos : linkedInvs) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            IItemHandler handler = handlerAt(pos);
            if (handler != null && ItemHandlerHelper.insertItemStacked(handler, copy.copy(), true).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    private @Nullable IItemHandler handlerAt(BlockPos pos) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
    }

    private ParticleColor particleColor() {
        return ArsDecoCompat.dyeColor(this)
                .map(dye -> ParticleColor.fromInt(dye.getTextureDiffuseColor()))
                .orElseGet(ParticleColor::defaultParticleColor);
    }

    private void setStatus(Status newStatus) {
        if (status != newStatus) {
            status = newStatus;
            updateBlock();
        }
    }

    public Status getStatus() {
        return status;
    }

    // ---------------------------------------------------------------- template

    public ItemStack getTemplate() {
        return template;
    }

    /** Replacing or removing the template discards the source stored toward copying it. */
    public void setTemplate(ItemStack stack) {
        if (!template.isEmpty()) {
            getSourceStorage().setSource(0);
        }
        template = stack;
        tickCounter = 0;
        status = stack.isEmpty() ? Status.EMPTY : Status.WORKING;
        updateBlock();
    }

    // ---------------------------------------------------------------- Dominion Wand linking

    /** Vessel clicked first, then the inventory. */
    @Override
    public Result onFirstConnection(@Nullable GlobalPos storedPos, @Nullable Direction face,
                                    @Nullable LivingEntity storedEntity, Player player) {
        return toggleLink(storedPos, player);
    }

    /** Inventory clicked first, then the vessel. */
    @Override
    public Result onLastConnection(@Nullable GlobalPos storedPos, @Nullable Direction face,
                                   @Nullable LivingEntity storedEntity, Player player) {
        return toggleLink(storedPos, player);
    }

    private Result toggleLink(@Nullable GlobalPos target, Player player) {
        if (target == null || level == null || !target.dimension().equals(level.dimension())) {
            return Result.FAIL;
        }
        BlockPos pos = target.pos().immutable();
        if (pos.equals(worldPosition)) {
            return Result.FAIL;
        }
        if (handlerAt(pos) == null) {
            PortUtil.sendMessage(player, Component.translatable("resources.vessel.not_inventory"));
            return Result.FAIL;
        }
        int range = ResourcesConfig.LINK_RANGE.get();
        if (!pos.closerThan(worldPosition, range + 0.5)) {
            PortUtil.sendMessage(player, Component.translatable("resources.vessel.too_far", range));
            return Result.FAIL;
        }
        if (linkedInvs.remove(pos)) {
            PortUtil.sendMessage(player, Component.translatable("resources.vessel.removed"));
        } else {
            linkedInvs.add(pos);
            PortUtil.sendMessage(player, Component.translatable("resources.vessel.bound"));
        }
        updateBlock();
        return Result.SUCCESS;
    }

    /** Sneak-click with the wand clears every link. */
    @Override
    public Result onClearConnections(Player player) {
        if (linkedInvs.isEmpty()) {
            return Result.NONE;
        }
        linkedInvs.clear();
        PortUtil.sendMessage(player, Component.translatable("resources.vessel.cleared"));
        updateBlock();
        return Result.CLEAR;
    }

    @Override
    public List<ColorPos> getWandHighlight(List<ColorPos> list) {
        for (BlockPos pos : linkedInvs) {
            list.add(ColorPos.centered(pos, ParticleColor.FROM_HIGHLIGHT));
        }
        return list;
    }

    // ---------------------------------------------------------------- look-at tooltip

    @Override
    public void getTooltip(List<Component> tooltip) {
        if (template.isEmpty()) {
            tooltip.add(Component.translatable("resources.vessel.tooltip.empty"));
            return;
        }
        tooltip.add(Component.translatable("resources.vessel.tooltip.copying", template.getHoverName()));
        int cost = VesselCosts.costFor(template);
        if (cost > 0) {
            tooltip.add(Component.translatable("resources.vessel.tooltip.source", Math.min(getSource(), cost), cost));
        }
        tooltip.add(Component.translatable("resources.vessel.tooltip.links", linkedInvs.size()));
        if (status.isPaused()) {
            tooltip.add(Component.translatable("resources.vessel.status." + status.name().toLowerCase())
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    // ---------------------------------------------------------------- save / sync

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (!template.isEmpty()) {
            tag.put(TEMPLATE_TAG, template.save(registries));
        }
        ListTag links = new ListTag();
        for (BlockPos pos : linkedInvs) {
            links.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put(LINKS_TAG, links);
        tag.putString(STATUS_TAG, status.name());
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        template = tag.contains(TEMPLATE_TAG)
                ? ItemStack.parse(registries, tag.getCompound(TEMPLATE_TAG)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        linkedInvs.clear();
        for (Tag entry : tag.getList(LINKS_TAG, Tag.TAG_LONG)) {
            linkedInvs.add(BlockPos.of(((LongTag) entry).getAsLong()));
        }
        try {
            status = Status.valueOf(tag.getString(STATUS_TAG));
        } catch (IllegalArgumentException e) {
            status = template.isEmpty() ? Status.EMPTY : Status.WORKING;
        }
    }

    // ---------------------------------------------------------------- Container (one slot: the template)

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return template.isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return slot == 0 ? template : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        return removeItemNoUpdate(slot);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0 || template.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = template;
        setTemplate(ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot == 0) {
            setTemplate(stack);
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        template = ItemStack.EMPTY;
    }
}
