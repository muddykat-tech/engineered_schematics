package tech.muddykat.engineered_schematics.event;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;
import tech.muddykat.engineered_schematics.item.ESSchematicSettings;
import tech.muddykat.engineered_schematics.item.SchematicProjection;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

public class SchematicPickBlockHandler {

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void handlePickSchematicBlock(InputEvent.MouseButton.Pre event)
    {
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && event.getAction() == GLFW.GLFW_PRESS) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;

            if (player != null && mc.hitResult instanceof BlockHitResult hit &&
                    player.getOffhandItem().is(ESRegistry.SCHEMATIC_ITEM.get())) {

                ItemStack schematic = player.getOffhandItem();
                Level world = player.level();
                ESSchematicSettings settings = new ESSchematicSettings(schematic);

                if (settings.getMultiblock() == null || !settings.isPlaced() || settings.getPos() == null) return;

                MultiblockHandler.IMultiblock multiblock = settings.getMultiblock();
                Vec3i size = multiblock.getSize(world);
                BlockPos structure_placed_at = settings.getPos();

                SchematicProjection projection = new SchematicProjection(world, multiblock);
                projection.setFlip(settings.isMirrored());
                projection.setRotation(settings.getRotation());

                // Find the current working layer by checking from bottom-up
                int workingLayer = 0;

                for (int y = 0; y < size.getY(); y++) {
                    int finalY = y;
                    boolean layerComplete = projection.process(y, p ->
                    {
                        BlockPos structure_block_world_position = structure_placed_at.offset(p.tPos);

                        if (structure_block_world_position.getY() == (structure_placed_at.getY() + finalY)) {
                            BlockState worldState = world.getBlockState(structure_block_world_position);
                            return !worldState.getBlock().equals(p.tBlockInfo.state().getBlock());
                        }

                        return false;
                    });

                    if (layerComplete) {
                        workingLayer = y;
                        break;
                    }
                }

                projection.process(workingLayer, p ->
                {
                    ItemStack stack = ItemStack.EMPTY;
                    BlockPos structure_block_world_position = structure_placed_at.offset(p.tPos);
                    BlockPos hitPos = hit.getBlockPos();
                    Vec3i offset = hit.getDirection().getNormal();
                    hitPos = hitPos.offset(offset);

                    // If the hit block is the same as the structure block
                    if (structure_block_world_position.equals(hitPos) && world.getBlockState(structure_block_world_position).isAir()) {
                        // Find the structure block directly above
                        stack = new ItemStack(p.tBlockInfo.state().getBlock().asItem()).copy();
                    }

                    // Ensure missing parts of the structure aren't picked
                    if (!stack.isEmpty()) {
                        Inventory inventory = player.getInventory();
                        int i = inventory.findSlotMatchingItem(stack);
                        if (player.getAbilities().instabuild) {
                            inventory.setPickedItem(stack);
                            mc.gameMode.handleCreativeModeItemAdd(player.getItemInHand(InteractionHand.MAIN_HAND), 36 + inventory.selected);
                        } else if (i != -1) {
                            if (Inventory.isHotbarSlot(i)) {
                                inventory.selected = i;
                            } else {
                                mc.gameMode.handlePickItem(i);
                            }
                        }
                        event.setCanceled(true);
                        return true;
                    }
                    return false;
                });
            }
        }
    }
}
