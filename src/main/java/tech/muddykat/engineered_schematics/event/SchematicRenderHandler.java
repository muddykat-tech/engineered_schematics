package tech.muddykat.engineered_schematics.event;

import blusunrize.immersiveengineering.client.BlockOverlayUtils;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.items.EngineersBlueprintItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.block.entity.SchematicBoardBlockEntity;
import tech.muddykat.engineered_schematics.helper.SchematicRenderer;
import tech.muddykat.engineered_schematics.item.ESSchematicSettings;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static tech.muddykat.engineered_schematics.item.SchematicItem.getSettings;

@Mod.EventBusSubscriber(modid = EngineeredSchematics.MODID, value = Dist.CLIENT)
public class SchematicRenderHandler {
    // How far from a corkboard players still see the projections pinned to it
    private static final double BOARD_PROJECTION_RANGE = 64;
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
        Set<String> rendered = new HashSet<>();
        matrix.pushPose();
        {
            Vec3 renderView = mc.gameRenderer.getMainCamera().getPosition();
            matrix.translate(-renderView.x, -renderView.y, -renderView.z);
            // Only a schematic in the offhand switches to the full projection; any other offhand item with NBT used to hide the grid
            if(hasSettings(secondItem, schematic))
            {
                // Allows multiple schematics to be visible at once as long as they're in the hot bar.
                for(int i = 0;i <= Inventory.getSelectionSize();i++){
                    ItemStack stack = (i == Inventory.getSelectionSize() ? secondItem : mc.player.getInventory().getItem(i));
                    if(hasSettings(stack, schematic))
                    {
                        ESSchematicSettings settings =  new ESSchematicSettings(stack);
                        if(!rendered.add(projectionKey(settings))) continue;
                        matrix.pushPose();
                        SchematicRenderer.renderSchematic(matrix, settings, mc.player, mc.player.level());
                        matrix.popPose();
                    }
                }
            }
            else if(hasSettings(mainItem, schematic))
            {
                for(int i = 0;i < Inventory.getSelectionSize();i++) {
                    ItemStack stack = mc.player.getInventory().getItem(i);
                    if (hasSettings(stack, schematic)) {
                        ESSchematicSettings settings = new ESSchematicSettings(stack);
                        matrix.pushPose();
                        SchematicRenderer.renderSchematicGrid(matrix, settings, mainItem == stack ? 0xffffff : 0x666666, mc.player.level());
                        matrix.popPose();
                    }
                }
            }
            renderBoardProjections(matrix, mc, schematic, rendered);
        }
        matrix.popPose();
    }

    private static void renderBoardProjections(PoseStack matrix, Minecraft mc, Item schematic, Set<String> rendered)
    {
        Player player = mc.player;
        for(SchematicBoardBlockEntity board : SchematicBoardBlockEntity.getClientBoards())
        {
            // Entries from a previous world can linger until garbage collected
            if(board.isRemoved() || board.getLevel() != player.level()) continue;
            if(!board.getBlockPos().closerToCenterThan(player.position(), BOARD_PROJECTION_RANGE)) continue;
            for(ItemStack stack : board.getInventory())
            {
                if(!hasSettings(stack, schematic)) continue;
                ESSchematicSettings settings = new ESSchematicSettings(stack);
                if(settings.getMultiblock() == null || settings.getPos() == null || !rendered.add(projectionKey(settings))) continue;
                matrix.pushPose();
                SchematicRenderer.renderSchematic(matrix, settings, player, player.level());
                matrix.popPose();
            }
        }
    }

    // Identifies a projection, so the same one pinned to several boards or also held is only drawn once
    private static String projectionKey(ESSchematicSettings settings)
    {
        String multiblock = settings.getMultiblock() == null ? "" : settings.getMultiblock().getUniqueName().toString();
        return multiblock + "@" + settings.getPos() + "/" + settings.getRotation() + "/" + settings.isMirrored();
    }

    private static boolean hasSettings(ItemStack stack, Item schematic)
    {
        return stack.is(schematic) && stack.hasTag() && stack.getTag().contains("settings", Tag.TAG_COMPOUND);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.ITEM_NAME.id())) {
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
