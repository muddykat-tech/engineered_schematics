package tech.muddykat.engineered_schematics.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tech.muddykat.engineered_schematics.block.entity.SchematicBoardBlockEntity;

public class CorkboardRenderer implements BlockEntityRenderer<SchematicBoardBlockEntity> {
    @Override
    public void render(SchematicBoardBlockEntity tile, float pPartialTick, PoseStack pose, MultiBufferSource mainBuffer, int pPackedLight, int pPackedOverlay) {
        NonNullList<ItemStack> inventory = tile.getInventory();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        Level level = tile.getLevel();
        assert inventory != null;
        if(!inventory.isEmpty())
        {
            pose.scale(0.5f,0.5f,0.5f);
            float xPos = .5f;
            float yPos = 1.5f;
            for(ItemStack item : inventory)
            {
                if(!item.isEmpty()) {
                    pose.pushPose();
                    pose.translate(xPos, yPos, .3f + (xPos*0.01));
                    itemRenderer.renderStatic(
                            item, ItemDisplayContext.FIXED,
                            pPackedLight, pPackedOverlay, pose, mainBuffer,
                            level, 0
                    );
                    xPos += 0.5f;
                    pose.popPose();
                }
            }
        }
    }
}
