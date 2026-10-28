package tech.muddykat.engineered_schematics.block.entity;

import tech.muddykat.engineered_schematics.block.SchematicDeskBlock;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IGuiTile;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHasObjProperty;
import blusunrize.immersiveengineering.common.blocks.TileEntityIEBase;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import com.google.common.collect.Lists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

public class SchematicTableBlockEntity extends TileEntityIEBase implements IIEInventory, IGuiTile, IHasObjProperty {
    public static final int GUI_ID = 0;
    private static final ArrayList<String> NO_PAPER = Lists.newArrayList("base_model");
    private static final ArrayList<String> SOME_PAPER = Lists.newArrayList("base_model", "scroll_1");
    private static final ArrayList<String> PAPER = Lists.newArrayList("base_model", "scroll_1", "scroll_2");
    private static final ArrayList<String> MUCH_PAPER = Lists.newArrayList("base_model", "scroll_1", "scroll_2", "scroll_3");
    private NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
    @SideOnly(Side.CLIENT)
    private AxisAlignedBB renderAABB;

    @Override public void readCustomNBT(NBTTagCompound nbt, boolean descPacket) { this.inventory = Utils.readInventory(nbt.getTagList("inventory", 10), 3); }

    @Override public void writeCustomNBT(NBTTagCompound nbt, boolean descPacket) { nbt.setTag("inventory", Utils.writeInventory(this.inventory)); }

    @SideOnly(Side.CLIENT)
    @Override @Nonnull public AxisAlignedBB getRenderBoundingBox() {
        if (this.renderAABB == null) { this.renderAABB = new AxisAlignedBB(getPos().getX() - 1, getPos().getY(), getPos().getZ() - 1, getPos().getX() + 2, getPos().getY() + 2, getPos().getZ() + 2); }
        return this.renderAABB;
    }

    @Override public NonNullList<ItemStack> getInventory() { return this.inventory; }

    @Override public boolean isStackValid(int slot, ItemStack stack) { return true; }

    @Override public int getSlotLimit(int slot) { return 64; }

    @Override public void doGraphicalUpdates(int slot) { markContainingBlockForUpdate(null); }

    private IBlockState state() { return world.getBlockState(this.pos); }

    public EnumFacing getFacing() {
        IBlockState state = state();
        return state.getBlock() instanceof SchematicDeskBlock ? state.getValue(SchematicDeskBlock.FACING) : EnumFacing.NORTH;
    }

    public boolean isDummy() {
        IBlockState state = state();
        return state.getBlock() instanceof SchematicDeskBlock && state.getValue(SchematicDeskBlock.DUMMY);
    }

    public EnumFacing getDummyDirection() {
        IBlockState state = state();
        return state.getBlock() instanceof SchematicDeskBlock ? SchematicDeskBlock.partnerDirection(state) : EnumFacing.EAST;
    }

    public void placeDummy() {
        if (isDummy()) { return; }
        BlockPos dummyPos = this.pos.offset(getDummyDirection());
        if (!world.getBlockState(dummyPos).getBlock().isReplaceable(world, dummyPos)) { return; }
        world.setBlockState(dummyPos, state().withProperty(SchematicDeskBlock.DUMMY, true));
    }

    @Override public boolean canOpenGui() { return true; }

    @Override public int getGuiID() { return GUI_ID; }

    @Nullable
    @Override public SchematicTableBlockEntity getGuiMaster() {
        if (!isDummy()) { return this; }
        TileEntity te = world.getTileEntity(pos.offset(getDummyDirection()));
        return te instanceof SchematicTableBlockEntity ? (SchematicTableBlockEntity)te : null;
    }

    @Override @Nonnull public ArrayList<String> compileDisplayList() {
        int paper = this.inventory.get(0).getCount();
        if (paper > 32) { return MUCH_PAPER; }
        if (paper > 15) { return PAPER; }
        if (paper > 0) { return SOME_PAPER; }
        return NO_PAPER;
    }
}
