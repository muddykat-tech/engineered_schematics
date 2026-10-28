package tech.muddykat.engineered_schematics.block;

import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.block.entity.SchematicBoardBlockEntity;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SchematicCorkboardBlock extends Block {
    public static final PropertyDirection FACING = IEProperties.FACING_HORIZONTAL;
    private static final AxisAlignedBB NORTH_AABB = new AxisAlignedBB(0.0, 0.0, 0.8125, 1.0, 1.0, 1.0);
    private static final AxisAlignedBB SOUTH_AABB = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 0.1875);
    private static final AxisAlignedBB WEST_AABB = new AxisAlignedBB(0.8125, 0.0, 0.0, 1.0, 1.0, 1.0);
    private static final AxisAlignedBB EAST_AABB = new AxisAlignedBB(0.0, 0.0, 0.0, 0.1875, 1.0, 1.0);

    public SchematicCorkboardBlock() {
        super(Material.WOOD);
        setRegistryName(EngineeredSchematics.MODID, "corkboard");
        setTranslationKey(EngineeredSchematics.MODID + ".corkboard");
        setCreativeTab(EngineeredSchematics.CREATIVE_TAB);
        setSoundType(SoundType.WOOD);
        setHardness(1.0F);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    public static EnumFacing facingOf(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof SchematicCorkboardBlock ? state.getValue(FACING) : EnumFacing.NORTH;
    }

    @Override @Nonnull protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING); }

    @SuppressWarnings("deprecation")
    @Override @Nonnull public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta)); }

    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getHorizontalIndex(); }

    @Override @Nonnull public IBlockState getStateForPlacement(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ, int meta, @Nonnull EntityLivingBase placer, @Nonnull EnumHand hand) {
        EnumFacing horizontal = facing.getAxis().isHorizontal() ? facing : placer.getHorizontalFacing().getOpposite();
        return getDefaultState().withProperty(FACING, placer.isSneaking() ? horizontal.getOpposite() : horizontal);
    }

    @SuppressWarnings("deprecation")
    @Override @Nonnull public AxisAlignedBB getBoundingBox(IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        switch (state.getValue(FACING)) {
            case NORTH: return NORTH_AABB;
            case SOUTH: return SOUTH_AABB;
            case WEST: return WEST_AABB;
            default: return EAST_AABB;
        }
    }

    @Override public boolean hasTileEntity(@Nonnull IBlockState state) { return true; }

    @Nullable
    @Override public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) { return new SchematicBoardBlockEntity(); }

    @SuppressWarnings("deprecation")
    @Override public boolean isOpaqueCube(@Nonnull IBlockState state) { return false; }

    @SuppressWarnings("deprecation")
    @Override public boolean isFullCube(@Nonnull IBlockState state) { return false; }

    @Override public void onBlockPlacedBy(World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityLivingBase placer, @Nonnull ItemStack stack) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof SchematicBoardBlockEntity) { ((SchematicBoardBlockEntity)te).onInitialPlace(); }
    }

    @Override public void onNeighborChange(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull BlockPos neighbor) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof SchematicBoardBlockEntity) { ((SchematicBoardBlockEntity)te).updateEdges(neighbor); }
    }

    @SuppressWarnings("deprecation")
    @Override public void neighborChanged(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Block block, @Nonnull BlockPos fromPos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof SchematicBoardBlockEntity) { ((SchematicBoardBlockEntity)te).updateEdges(fromPos); }
    }

    @Override public boolean onBlockActivated(World world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer player, @Nonnull EnumHand hand, @Nonnull EnumFacing side, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof SchematicBoardBlockEntity)) { return false; }
        return ((SchematicBoardBlockEntity)te).interact(side, player, hand, player.getHeldItem(hand), hitX, hitY, hitZ);
    }

    @Override public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof SchematicBoardBlockEntity) {
            for (ItemStack stack : ((SchematicBoardBlockEntity)te).getInventory()) {
                if (!stack.isEmpty()) { Utils.dropStackAtPos(world, pos, stack); }
            }
        }
        super.breakBlock(world, pos, state);
    }
}
