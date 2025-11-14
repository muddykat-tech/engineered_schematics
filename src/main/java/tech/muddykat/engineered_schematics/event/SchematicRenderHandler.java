package tech.muddykat.engineered_schematics.event;

import blusunrize.immersiveengineering.client.ClientUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.helper.SchematicRenderer;
import tech.muddykat.engineered_schematics.item.ESSchematicSettings;
import tech.muddykat.engineered_schematics.registry.ESDataComponents;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

@EventBusSubscriber(modid = EngineeredSchematics.MODID, value = Dist.CLIENT)
public class SchematicRenderHandler {
    @SubscribeEvent
    public static void renderLevelStage(RenderLevelStageEvent event)
    {
        if(event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS)
        {
            renderMultiblockSchematic(event);
        }
    }

    private static void renderMultiblockSchematic(RenderLevelStageEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null) return;
        Item schematic = ESRegistry.SCHEMATIC_ITEM.get();
        ItemStack mainItem = mc.player.getMainHandItem();
        ItemStack secondItem = mc.player.getOffhandItem();
        PoseStack matrix = event.getPoseStack();
        matrix.pushPose();
        {
            Vec3 renderView = mc.gameRenderer.getMainCamera().getPosition();
            matrix.translate(-renderView.x, -renderView.y, -renderView.z);
            if(secondItem.has(ESDataComponents.SCHEMATIC_PROJECTION_DATA))
            {
                // Allows multiple schematics to be visible at once as long as they're in the hot bar.
                for(int i = 0;i <= 10;i++){
                    ItemStack stack = (i == 10 ? secondItem : mc.player.getInventory().getItem(i));
                    if(stack.is(schematic) && secondItem.has(ESDataComponents.SCHEMATIC_PROJECTION_DATA))
                    {
                        matrix.pushPose();
                        {
                            ESSchematicSettings settings =  new ESSchematicSettings(stack);
                            SchematicRenderer.renderSchematic(matrix, settings, mc.player, mc.player.level());
                        }
                        matrix.popPose();
                    }
                }
            }
            else
            if(mainItem.has(ESDataComponents.SCHEMATIC_PROJECTION_DATA))
            {
                for(int i = 0;i <= 9;i++) {
                    ItemStack stack = mc.player.getInventory().getItem(i);
                    if (mainItem.is(schematic)) {
                        matrix.pushPose();
                        ESSchematicSettings settings = new ESSchematicSettings(stack);
                        SchematicRenderer.renderSchematicGrid(matrix, settings, mainItem.equals(stack) ? 0xffffff : 0x666666, mc.player.level());
                        matrix.popPose();
                    }
                }
            }
        }
        matrix.popPose();
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) {
            return;
        }

        Minecraft mc = ClientUtils.mc();
        Player player = mc.player;
        if (player == null || mc.hitResult == null) {
            return;
        }

        HitResult mop = mc.hitResult;
        if (!(mop instanceof BlockHitResult blockHit)) {
            return;
        }
    }
}
