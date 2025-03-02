package tech.muddykat.engineered_schematics.item;

import blusunrize.immersiveengineering.api.crafting.BlueprintCraftingRecipe;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.common.items.EngineersBlueprintItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiPredicate;

public class SchematicItem extends Item {
    public SchematicItem() {
        super(new Properties().stacksTo(1));
    }


    @Override
    public @NotNull Component getName(ItemStack stack)
    {
        String selfKey = getDescriptionId(stack);
        if(stack.hasTag())
        {
            ESSchematicSettings settings = getSettings(stack);
            if(settings.getMultiblock() != null)
            {
                Component name = settings.getMultiblock().getDisplayName();
                String key = selfKey+".specific" + (settings.isMirrored() ? ".mirrored" : "");
                return Component.translatable(key, name).withStyle(ChatFormatting.AQUA);
            }
        }
        return Component.translatable(selfKey).withStyle(ChatFormatting.AQUA);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag pIsAdvanced)
    {
        ESSchematicSettings settings = getSettings(stack);
        if(settings.getMultiblock() != null){
            assert worldIn!=null;
            Vec3i size = settings.getMultiblock().getSize(worldIn);
            tooltip.add(Component.translatable("desc.engineered_schematics.info.schematic.size", Component.literal("["+ size.getX() +"x" + size.getY() +"x" + size.getZ()+ "]")));
            MultiblockHandler.IMultiblock mb = settings.getMultiblock();
            String machine_tier_id = "desc.engineered_schematics.info.schematic.ie_tier";
            tooltip.add(Component.translatable("desc.engineered_schematics.info.schematic.tier", Component.translatable(machine_tier_id).withStyle(ChatFormatting.AQUA)));
            tooltip.add(Component.translatable("desc.engineered_schematics.info.schematic.block_info", Component.keybind("shift").withStyle(ChatFormatting.GOLD)));
        } else{
            tooltip.add(Component.translatable("desc.engineered_schematics.info.schematic.no_multiblock"));
        }
    }

    public static ESSchematicSettings getSettings(@Nullable ItemStack stack){
        return new ESSchematicSettings(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Player player = context.getPlayer();
        if(player == null) return InteractionResult.SUCCESS;
        Level level = context.getLevel();
        InteractionHand hand = context.getHand();
        Direction facing = context.getClickedFace();
        BlockPos pos = context.getClickedPos().above();
        ItemStack stack = player.getItemInHand(hand);
        ESSchematicSettings settings = getSettings(stack);
        // TODO allow configuration options
        if(hand.equals(InteractionHand.MAIN_HAND))
        {
            settings.setPos(pos);
            settings.setPlaced(true);
            settings.applyTo(stack);
            if(player.isShiftKeyDown())
            {
                Rotation rot = player.getDirection().equals(Direction.NORTH) ? Rotation.NONE : (player.getDirection().equals(Direction.EAST) ? Rotation.CLOCKWISE_90 : (player.getDirection().equals(Direction.WEST) ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_180));
                settings.setRotation(rot);
                settings.applyTo(stack);
                player.displayClientMessage(Component.translatable("desc.engineered_schematics.info.schematic.rotated"), true);
                player.displayClientMessage(Component.translatable("desc.engineered_schematics.info.schematic.moved"), true);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        ESSchematicSettings settings = getSettings(stack);
        BlockPos pos = settings.getPos();
        if(player.isShiftKeyDown() && player.isCreative() && pos != null && settings.getMultiblock() != null)
        {
            final BlockPos.MutableBlockPos hit = pos.mutable();
            if(!level.isClientSide)
            {
                // Creative Placement
                BiPredicate<Integer, SchematicProjection.Info> pred = (layer, info) -> {
                    BlockPos realPos = info.tPos.offset(hit);
                    BlockState to_state = info.getModifiedState(level, realPos);
                    level.setBlockAndUpdate(realPos, to_state);
                    return false; // Don't ever skip a step.
                };

                SchematicProjection projection = new SchematicProjection(level, settings.getMultiblock());
                projection.setFlip(settings.isMirrored());
                projection.setRotation(settings.getRotation());
                projection.processAll(pred);

                player.displayClientMessage(Component.translatable("desc.engineered_schematics.info.schematic.placed"), true);

                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }
}
