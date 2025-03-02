package tech.muddykat.engineered_schematics.event;

import blusunrize.immersiveengineering.client.BlockOverlayUtils;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.blocks.wooden.BlueprintShelfBlockEntity;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay;
import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.helper.SchematicRenderer;
import tech.muddykat.engineered_schematics.item.ESSchematicSettings;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import java.util.List;

import static tech.muddykat.engineered_schematics.item.SchematicItem.getSettings;

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
                for(int i = 0;i <= 9;i++) {
                    ItemStack stack = mc.player.getInventory().getItem(i);
                    if (mainItem.is(schematic) && mainItem.hasTag() && mainItem.getTag().contains("settings", Tag.TAG_COMPOUND)) {
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

//        BlockPos pos = blockHit.getBlockPos();
//        BlockEntity tileEntity = player.level().getBlockEntity(pos);
//        if (tileEntity instanceof BlueprintShelfBlockEntity entity) {
//            renderBlueprintShelfOverlay(entity, blockHit, event.getGuiGraphics().pose(),
//                    mc.getWindow().getGuiScaledWidth(),
//                    mc.getWindow().getGuiScaledHeight());
//        }
    }

    public static Component[] getOverlayText(BlueprintShelfBlockEntity entity, Player player, HitResult mop) {
        if (!(mop instanceof BlockHitResult bhr) || entity.getInventory() == null) {
            return null;
        }

        float hitX = (float) bhr.getLocation().x - (float) bhr.getBlockPos().getX();
        float hitY = (float) bhr.getLocation().y - (float) bhr.getBlockPos().getY();
        float hitZ = (float) bhr.getLocation().z - (float) bhr.getBlockPos().getZ();

        int targetedSlot = getTargetedSlot(bhr.getDirection(), hitX, hitY, hitZ);
        ItemStack stack = entity.getInventory().get(targetedSlot);

        String selfKey = stack.getDescriptionId();
        if (!stack.hasTag()) {
            return null;
        }

        ESSchematicSettings settings = getSettings(stack);
        if (settings.getMultiblock() == null) {
            return null;
        }

        Component name = settings.getMultiblock().getDisplayName();
        String key = selfKey + ".specific" + (settings.isMirrored() ? ".mirrored" : "");
        return new Component[]{Component.translatable(key, name)};
    }

    private static int getTargetedSlot(Direction side, float hitX, float hitY, float hitZ) {
        float targetU;

        switch (side) {
            case SOUTH -> targetU = hitX;
            case WEST -> targetU = hitZ;
            case EAST -> targetU = 1.0F - hitZ;
            default -> targetU = 1.0F - hitX; // NORTH, UP, DOWN
        }

        float targetV = side == Direction.UP ? hitZ : hitY;
        int row = (targetV < 0.33) ? 2 : (targetV < 0.66 ? 1 : 0);
        int col = (int)Math.floor(targetU * 3.0F);

        return col + row * 3;
    }

    private static void renderSchematicOverlay(BlueprintShelfBlockEntity be, HitResult mop,
                                                    PoseStack transform, int scaledWidth, int scaledHeight) {
        final Component[] overlayText = getOverlayText(be, ClientUtils.mc().player, mop);
        if (overlayText != null) {
            BlockOverlayUtils.drawBlockOverlayText(transform, overlayText, scaledWidth, scaledHeight);
        }
    }
}
