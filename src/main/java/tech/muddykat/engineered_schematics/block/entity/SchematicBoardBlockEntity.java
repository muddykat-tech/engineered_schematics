package tech.muddykat.engineered_schematics.block.entity;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.common.blocks.IEBaseBlockEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.PlacementLimitation;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.block.SchematicCorkBoard;
import tech.muddykat.engineered_schematics.client.renderer.BorderState;
import tech.muddykat.engineered_schematics.item.SchematicItem;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import java.util.*;
import java.util.function.Consumer;

public class SchematicBoardBlockEntity
        extends IEBaseBlockEntity implements IIEInventory, IEBlockInterfaces.IBlockEntityDrop,
        IEBlockInterfaces.IStateBasedDirectional, IEBlockInterfaces.IBlockOverlayText
{
    public static final int NUM_SLOTS = 4;

    BorderState borderState = new BorderState();

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(NUM_SLOTS, ItemStack.EMPTY);

    public SchematicBoardBlockEntity(BlockPos pos, BlockState state)
    {
        super(ESRegistry.SCHEMATIC_BOARD_TYPE.get(), pos, state);
        rand = new Random(getBlockPos().asLong());
        while(randomStates.size() < 8) randomStates.add(rand.nextFloat());
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
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        borderState.writeToNBT(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        borderState.updateFromNBT(nbt);
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket)
    {
        ContainerHelper.loadAllItems(nbt, inventory);
        borderState.updateFromNBT(nbt);
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket) {
        ContainerHelper.saveAllItems(nbt, inventory);
        borderState.writeToNBT(nbt);
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

    public static int getTargetedSlot(Direction side, float hitX, float hitY, float hitZ)
    {
        float targetU = side==Direction.NORTH ? (1-hitX) : (side==Direction.SOUTH ? (hitX) : (side==Direction.EAST ? 1-hitZ : (hitZ)));
        float targetV = side==Direction.UP?1-hitZ: 1-hitY;
        return targetU < 0.5 ? (targetV < 0.5 ? 0 : 2) : ((targetV < 0.5 ? 1 : 3));
    }

    public InteractionResult interact(Direction side, Player player, InteractionHand hand, ItemStack heldItem, float hitX, float hitY, float hitZ)
    {
        int targetedSlot = getTargetedSlot(side, hitX, hitY, hitZ);
        ItemStack stackInSlot = this.inventory.get(targetedSlot);
        if(!stackInSlot.isEmpty())
        {
            if(heldItem.isEmpty())
                player.setItemInHand(hand, stackInSlot);
            else if(!getLevelNonnull().isClientSide())
                player.spawnAtLocation(stackInSlot, 0);
            this.inventory.set(targetedSlot, ItemStack.EMPTY);
            return InteractionResult.sidedSuccess(getLevelNonnull().isClientSide);
        }
        else if(isStackValid(targetedSlot, heldItem))
        {
            this.inventory.set(targetedSlot, heldItem.copyWithCount(1));
            heldItem.shrink(1);
            return InteractionResult.sidedSuccess(getLevelNonnull().isClientSide);
        }
        markChunkDirty();
        markBlockForUpdate(getBlockPos(), getBlockState());
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
            if(stackInSlot.isEmpty()) return new Component[]{Component.empty()};
            return new Component[]{stackInSlot.getHoverName()};
        }
        return null;
    }
    // Cached map of adjacent blocks of the same type
    private final Map<Direction, Boolean> adjacentBlocks = new EnumMap<>(Direction.class);

    // Cached map of diagonal blocks (position -> hasSameBlock)
    private final Map<BlockPos, Boolean> diagonalBlocks = new HashMap<>();

    private boolean isSameBlockType(BlockPos pos) {
        assert level != null;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof SchematicBoardBlockEntity;
    }

    public void refreshAdjacentBlocks() {
        Direction facing = getFacing();

        // Check all six directions for direct adjacency
        for (Direction dir : Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(dir);
            boolean hasSameBlock = isSameBlockType(adjacentPos);
            adjacentBlocks.put(dir, hasSameBlock);

            // Update edge visibility (edge is visible when no adjacent block)
            borderState.updateEdge(dir, !hasSameBlock, facing);
        }

        // Check diagonal positions
        updateDiagonalBlocks();

        // Update corners based on adjacent and diagonal blocks
        borderState.updateCorners(adjacentBlocks, diagonalBlocks, facing, worldPosition);

        // Mark block for update to refresh rendering
        markBlockForUpdate(worldPosition, getBlockState());
    }

    private void updateDiagonalBlocks() {
        Direction facing = getFacing();
        diagonalBlocks.clear();

        // Check the four diagonal positions (top-left, top-right, bottom-right, bottom-left)
        checkAndCacheDiagonal(Direction.UP, facing.getClockWise());        // Top-right
        checkAndCacheDiagonal(Direction.UP, facing.getCounterClockWise());  // Top-left
        checkAndCacheDiagonal(Direction.DOWN, facing.getClockWise());      // Bottom-right
        checkAndCacheDiagonal(Direction.DOWN, facing.getCounterClockWise());// Bottom-left
    }

    private void checkAndCacheDiagonal(Direction dir1, Direction dir2) {
        BlockPos diagonalPos = worldPosition.relative(dir1).relative(dir2);
        boolean hasSameBlock = isSameBlockType(diagonalPos);
        diagonalBlocks.put(diagonalPos, hasSameBlock);
    }

    public void updateEdges(BlockPos neighborPos) {
        // Calculate if this is a direct neighbor or a diagonal neighbor
        BlockPos delta = neighborPos.subtract(worldPosition);
        int manhattan = Math.abs(delta.getX()) + Math.abs(delta.getY()) + Math.abs(delta.getZ());

        // For direct neighbors (manhattan distance = 1)
        if (manhattan == 1) {
            Direction side = Direction.getNearest(delta.getX(), delta.getY(), delta.getZ());

            // Update our cached state for this direction
            boolean hasSameBlock = isSameBlockType(neighborPos);
            adjacentBlocks.put(side, hasSameBlock);

            // Update the corresponding edge
            borderState.updateEdge(side, !hasSameBlock, getFacing());

            // Need to update diagonal info as well since the neighbor changed
            updateDiagonalBlocks();

            // Update corners based on new information
            borderState.updateCorners(adjacentBlocks, diagonalBlocks, getFacing(), worldPosition);

            // Refresh the block visually
            markBlockForUpdate(worldPosition, getBlockState());

            // Also notify diagonal and adjacent neighbors to update their corners
            notifyNeighborsToUpdate();
        }
        // For diagonal neighbors (manhattan distance = 2)
        else if (manhattan == 2 && delta.getZ() == 0) {
            // A diagonal neighbor changed, need to update our corners
            updateDiagonalBlocks();
            borderState.updateCorners(adjacentBlocks, diagonalBlocks, getFacing(), worldPosition);

            // Refresh the block visually
            markBlockForUpdate(worldPosition, getBlockState());
        }
    }
    private void notifyNeighborsToUpdate() {
        for (Direction dir : Direction.values()) {
            BlockPos adjacentPos = worldPosition.relative(dir);
            BlockEntity blockEntity = level.getBlockEntity(adjacentPos);

            if (blockEntity instanceof SchematicBoardBlockEntity board) {
                board.refreshAdjacentBlocks();
            }
        }
    }
    private static Random rand;
    public void onInitialPlace() {
        if(randomStates.isEmpty()) {
            rand = new Random(getBlockPos().asLong());
            while(randomStates.size() < 8) randomStates.add(rand.nextFloat());
        }
        // Refresh our own borders
        refreshAdjacentBlocks();

        // Notify all adjacent and diagonal blocks to update their borders
        notifyNeighborsToUpdate();

        // Also check diagonals and notify them
        Direction facing = getFacing();
        notifyDiagonalNeighbor(Direction.UP, facing.getClockWise());        // Top-right
        notifyDiagonalNeighbor(Direction.UP, facing.getCounterClockWise());  // Top-left
        notifyDiagonalNeighbor(Direction.DOWN, facing.getClockWise());      // Bottom-right
        notifyDiagonalNeighbor(Direction.DOWN, facing.getCounterClockWise());// Bottom-left
    }

    private void notifyDiagonalNeighbor(Direction dir1, Direction dir2) {
        BlockPos diagonalPos = worldPosition.relative(dir1).relative(dir2);
        BlockEntity blockEntity = level.getBlockEntity(diagonalPos);

        if (blockEntity instanceof SchematicBoardBlockEntity board) {
            board.refreshAdjacentBlocks();
        }
    }

    // Provide access to border state for rendering
    public BorderState getBorderState() {
        return borderState;
    }

    List<Float> randomStates = new ArrayList<>(8);
    public List<Float> getRandomState() {
        return randomStates;
    }
}