package tech.muddykat.engineered_schematics.block.entity;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.client.IModelOffsetProvider;
import blusunrize.immersiveengineering.common.blocks.IEBaseBlockEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.PlacementLimitation;
import blusunrize.immersiveengineering.common.blocks.wooden.DeskBlock;
import blusunrize.immersiveengineering.common.register.IEMenuTypes;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import tech.muddykat.engineered_schematics.helper.IInteractionObjectES;
import tech.muddykat.engineered_schematics.registry.ESMenuTypes;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import javax.annotation.Nullable;

public class SchematicTableBlockEntity extends IEBaseBlockEntity implements IIEInventory, IEBlockInterfaces.IStateBasedDirectional,
        IEBlockInterfaces.IHasDummyBlocks, IModelOffsetProvider, IInteractionObjectES<SchematicTableBlockEntity>
{
    public static final BlockPos MASTER_POS = BlockPos.ZERO;
    public static final BlockPos DUMMY_POS = new BlockPos(1, 0, 0);
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);

    public SchematicTableBlockEntity(BlockPos pos, BlockState state)
    {
        super(ESRegistry.SCHEMATIC_TABLE_TYPE.get(), pos, state);
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

    private AABB renderAABB;

    public AABB getRenderAABB() {
        if(renderAABB==null)
            renderAABB = new AABB(getBlockPos().getX()-1, getBlockPos().getY(), getBlockPos().getZ()-1, getBlockPos().getX()+2, getBlockPos().getY()+2, getBlockPos().getZ()+2);
        return renderAABB;
    }

    @Override
    public NonNullList<ItemStack> getInventory()
    {
        return this.inventory;
    }

    @Override
    public boolean isStackValid(int slot, ItemStack stack)
    {
        return true;
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return slot==0?1: 64;
    }

    @Override
    public void doGraphicalUpdates() {}

    @Override
    public @NotNull PlacementLimitation getFacingLimitation()
    {
        return PlacementLimitation.HORIZONTAL;
    }

    @Override
    public boolean canHammerRotate(Direction side, Vec3 hit, LivingEntity entity)
    {
        return false;
    }

    @Override
    public boolean isDummy()
    {
        return getState().getValue(IEProperties.MULTIBLOCKSLAVE);
    }

    @Nullable
    @Override
    public SchematicTableBlockEntity master()
    {
        if(!isDummy())
            return this;
        // Used to provide tile-dependant drops after breaking
        if(tempMasterBE!=null)
            return (SchematicTableBlockEntity)tempMasterBE;
        Direction dummyDir = isDummy()?getFacing().getCounterClockWise(): getFacing().getClockWise();
        BlockPos masterPos = getBlockPos().relative(dummyDir);
        BlockEntity te = Utils.getExistingTileEntity(level, masterPos);
        return (te instanceof SchematicTableBlockEntity)?(SchematicTableBlockEntity)te: null;
    }

    @Override
    public void placeDummies(BlockPlaceContext ctx, BlockState blockState) {
        DeskBlock.placeDummies(getBlockState(), level, worldPosition, ctx);
    }

    @Override
    public void breakDummies(BlockPos pos, BlockState state)
    {
        tempMasterBE = master();
        Direction dummyDir = isDummy()?getFacing().getCounterClockWise(): getFacing().getClockWise();
        level.removeBlock(pos.relative(dummyDir), false);
        if(inventory != null && !inventory.isEmpty()){
            for(ItemStack item : inventory)
            {
                Utils.dropStackAtPos(level, pos, item);
            }
        }
    }

    @Override
    public SchematicTableBlockEntity getGuiMaster()
    {
        if(!isDummy())
            return this;
        Direction dummyDir = getFacing().getCounterClockWise();
        BlockEntity tileEntityModWorkbench = level.getBlockEntity(worldPosition.relative(dummyDir));
        if(tileEntityModWorkbench instanceof SchematicTableBlockEntity)
            return (SchematicTableBlockEntity)tileEntityModWorkbench;
        return null;
    }

    @Override
    public ESMenuTypes.ArgContainer<? super SchematicTableBlockEntity, ?> getContainerType()
    {
        return ESMenuTypes.SCHEMATICS;
    }

    @Override
    public boolean canUseGui(Player var1)
    {
        return true;
    }

    @Override
    public @NotNull Property<Direction> getFacingProperty()
    {
        return IEProperties.FACING_HORIZONTAL;
    }

    @Override
    public BlockPos getModelOffset(BlockState blockState, Vec3i vec3i)
    {
        if(isDummy())
            return DUMMY_POS;
        else
            return MASTER_POS;
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("desc.engineered_schematics.schematic_table");
    }
}