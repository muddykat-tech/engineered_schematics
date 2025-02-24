package tech.muddykat.engineered_schematics.event;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.glfw.GLFW;
import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.helper.SchematicRenderer;
import tech.muddykat.engineered_schematics.item.ESSchematicSettings;
import tech.muddykat.engineered_schematics.item.SchematicProjection;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

@Mod.EventBusSubscriber(modid = EngineeredSchematics.MODID, value = Dist.CLIENT)
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
            if(secondItem.getTag()!=null)
            {

                // Allows multiple schematics to be visible at once as long as they're in the hot bar.
                for(int i = 0;i <= 10;i++){
                    ItemStack stack = (i == 10 ? secondItem : mc.player.getInventory().getItem(i));
                    if(stack.is(schematic) && secondItem.hasTag() && secondItem.getTag().contains("settings", Tag.TAG_COMPOUND))
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
            if(mainItem.getTag() != null)
            {
                boolean off = mainItem.is(schematic) && mainItem.hasTag() && mainItem.getTag().contains("settings", Tag.TAG_COMPOUND);
                if(off)
                {
                    matrix.pushPose();
                    {
                        ESSchematicSettings settings =  new ESSchematicSettings(mainItem);
                        SchematicRenderer.renderSchematicGrid(matrix, settings, mc.player.level());
                    }
                    matrix.popPose();
                }
            }
        }
        matrix.popPose();
    }
}
