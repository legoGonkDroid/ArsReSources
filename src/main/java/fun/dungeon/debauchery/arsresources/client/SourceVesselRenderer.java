package fun.dungeon.debauchery.arsresources.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fun.dungeon.debauchery.arsresources.block.SourceVesselTile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Floats the template item above the bowl, spinning like an item on an Arcane Pedestal, with a gentle bob. */
public class SourceVesselRenderer implements BlockEntityRenderer<SourceVesselTile> {
    public SourceVesselRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SourceVesselTile vessel, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        ItemStack template = vessel.getTemplate();
        if (template.isEmpty() || vessel.getLevel() == null) {
            return;
        }
        float time = vessel.getLevel().getGameTime() + partialTick;
        pose.pushPose();
        pose.translate(0.5, SourceVesselTile.ITEM_HEIGHT + Mth.sin(time / 12.0F) * 0.04, 0.5);
        pose.scale(0.45F, 0.45F, 0.45F);
        pose.mulPose(Axis.YP.rotationDegrees(time * 3.0F));
        Minecraft.getInstance().getItemRenderer().renderStatic(template, ItemDisplayContext.FIXED, packedLight,
                packedOverlay, pose, buffers, vessel.getLevel(), (int) vessel.getBlockPos().asLong());
        pose.popPose();
    }
}
