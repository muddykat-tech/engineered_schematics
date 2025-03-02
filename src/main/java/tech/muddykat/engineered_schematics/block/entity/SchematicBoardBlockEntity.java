package tech.muddykat.engineered_schematics.block.entity;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.common.blocks.IEBaseBlockEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.PlacementLimitation;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import tech.muddykat.engineered_schematics.block.SchematicCorkBoard;
import tech.muddykat.engineered_schematics.item.SchematicItem;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import java.util.function.Consumer;

public class SchematicBoardBlockEntity
        extends IEBaseBlockEntity implements IIEInventory, IEBlockInterfaces.IBlockEntityDrop,
        IEBlockInterfaces.IStateBasedDirectional, IEBlockInterfaces.IBlockOverlayText
{
    public static final int NUM_SLOTS = 9;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(NUM_SLOTS, ItemStack.EMPTY);

    public SchematicBoardBlockEntity(BlockPos pos, BlockState state)
    {
        super(ESRegistry.SCHEMATIC_BOARD_TYPE.get(), pos, state);
    }

    @Override
    public Property<Direction> getFacingProperty()
    {
        return IEProperties.FACING_HORIZONTAL;
    }

    @Override
    public PlacementLimitation getFacingLimitation()
    {
        return PlacementLimitation.PISTON_INVERTED_NO_DOWN;
    }

    @Override
    public boolean mirrorFacingOnPlacement(LivingEntity placer)
    {
        return placer.isShiftKeyDown();
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket)
    {
        ContainerHelper.loadAllItems(nbt, inventory);
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket)
    {
        ContainerHelper.saveAllItems(nbt, inventory);
    }

    @Override
    public void getBlockEntityDrop(LootContext context, Consumer<ItemStack> drop)
    {
        ItemStack stack = new ItemStack(getBlockState().getBlock(), 1);
        CompoundTag nbt = new CompoundTag();
        ContainerHelper.saveAllItems(nbt, inventory);
        if(!nbt.isEmpty())
            stack.setTag(nbt);
        drop.accept(stack);
    }

    @Override
    public void onBEPlaced(BlockPlaceContext ctx)
    {
        final ItemStack stack = ctx.getItemInHand();
        if(stack.hasTag())
            readCustomNBT(stack.getOrCreateTag(), false);
    }

    @Override
    public NonNullList<ItemStack> getInventory()
    {
        return inventory;
    }

    @Override
    public boolean isStackValid(int slot, ItemStack stack)
    {
        return stack.getItem() instanceof SchematicItem;
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return 1;
    }

    @Override
    public void doGraphicalUpdates()
    {
        this.setChanged();
        markContainingBlockForUpdate(null);
    }

    private static int getTargetedSlot(Direction side, float hitX, float hitY, float hitZ)
    {
        float targetU = switch(side)
        {
            case SOUTH -> hitX;
            case DOWN, UP, NORTH -> 1-hitX;
            case WEST -> hitZ;
            case EAST -> 1-hitZ;
        };
        float targetV = side==Direction.UP?hitZ: hitY;
        return (int)Math.floor(targetU*3)+(targetV < 0.33?6: targetV < 0.66?3: 0);
    }

    public InteractionResult interact(Direction side, Player player, InteractionHand hand, ItemStack heldItem, float hitX, float hitY, float hitZ)
    {
        int targetedSlot = getTargetedSlot(side, hitX, hitY, hitZ);
        ItemStack stackInSlot = this.inventory.get(targetedSlot);
        BlockState state = getLevelNonnull().getBlockState(getBlockPos());
        if(!stackInSlot.isEmpty())
        {
            if(heldItem.isEmpty())
                player.setItemInHand(hand, stackInSlot);
            else if(!getLevelNonnull().isClientSide())
                player.spawnAtLocation(stackInSlot, 0);
            this.inventory.set(targetedSlot, ItemStack.EMPTY);
            this.setState(state.setValue(SchematicCorkBoard.SCHEMATIC_SLOT_FILLED[targetedSlot], false));
            return InteractionResult.sidedSuccess(getLevelNonnull().isClientSide);
        }
        else if(isStackValid(targetedSlot, heldItem))
        {
            this.inventory.set(targetedSlot, heldItem.copyWithCount(1));
            heldItem.shrink(1);
            this.setState(state.setValue(SchematicCorkBoard.SCHEMATIC_SLOT_FILLED[targetedSlot], true));
            return InteractionResult.sidedSuccess(getLevelNonnull().isClientSide);
        }
        return InteractionResult.FAIL;
    }

    @Nullable
    @Override
    public Component[] getOverlayText(Player player, HitResult mop, boolean hammer)
    {
        if(mop instanceof BlockHitResult bhr)
        {
            final float hitX = (float)bhr.getLocation().x-bhr.getBlockPos().getX();
            final float hitY = (float)bhr.getLocation().y-bhr.getBlockPos().getY();
            final float hitZ = (float)bhr.getLocation().z-bhr.getBlockPos().getZ();
            int targetedSlot = getTargetedSlot(bhr.getDirection(), hitX, hitY, hitZ);
            ItemStack stackInSlot = this.inventory.get(targetedSlot);
            if(stackInSlot.isEmpty()) return null;
            return new Component[]{stackInSlot.getHoverName()};
        }
        return null;
    }
}