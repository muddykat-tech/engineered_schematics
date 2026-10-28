package tech.muddykat.engineered_schematics.block;

import blusunrize.immersiveengineering.api.IEProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import tech.muddykat.engineered_schematics.block.entity.SchematicBoardBlockEntity;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import javax.annotation.Nullable;

public class SchematicCorkBoard extends Block implements EntityBlock {
    public static final Property<Direction> FACING;

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable BlockGetter level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("desc.engineered_schematics.info.board.lore").withStyle(ChatFormatting.GRAY));
    }

    public SchematicCorkBoard() {
        super(Properties.of());
    }
    protected static final VoxelShape EAST_AABB = Block.box(0.0, 0.0, 0.0, 3.0, 16.0, 16.0);
    protected static final VoxelShape WEST_AABB = Block.box(13.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    protected static final VoxelShape SOUTH_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 3.0);
    protected static final VoxelShape NORTH_AABB = Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(new Property[]{FACING});
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult superResult = super.use(state, world, pos, player, hand, hit);

        if(player.isShiftKeyDown() && world.getBlockEntity(pos) instanceof SchematicBoardBlockEntity board)
        {
            if(hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if(!world.isClientSide)
                player.displayClientMessage(Component.translatable("desc.engineered_schematics.info.board.mode", board.cycleMode().getTranslated()), true);
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        if(superResult.consumesAction())
            return superResult;
        final Direction side = hit.getDirection();
        final float hitX = (float)hit.getLocation().x-pos.getX();
        final float hitY = (float)hit.getLocation().y-pos.getY();
        final float hitZ = (float)hit.getLocation().z-pos.getZ();
        ItemStack heldItem = player.getItemInHand(hand);
        BlockEntity tile = world.getBlockEntity(pos);
        if(tile instanceof SchematicBoardBlockEntity interaction)
        {
            InteractionResult res = interaction.interact(side,player,hand,heldItem,hitX,hitY,hitZ);
            if(res.consumesAction() || res==InteractionResult.FAIL) return res;
        }

        return superResult;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        switch((Direction)pState.getValue(FACING)) {
            case NORTH:
                return SOUTH_AABB;
            case SOUTH:
                return NORTH_AABB;
            case WEST:
                return EAST_AABB;
            case EAST:
            default:
                return WEST_AABB;
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        if (!pContext.replacingClickedOnBlock()) {
            BlockState blockstate = pContext.getLevel().getBlockState(pContext.getClickedPos().relative(pContext.getClickedFace().getOpposite()));
            if (blockstate.is(this) && blockstate.getValue(FACING) == pContext.getClickedFace()) {
                return null;
            }
        }

        BlockState blockstate = this.defaultBlockState();
        for(Direction direction : pContext.getNearestLookingDirections()) {
            if (Direction.Plane.HORIZONTAL.test(direction)) {
                blockstate = blockstate.setValue(FACING, direction.getOpposite());
            }
        }
        return blockstate;
    }

    static {
        FACING = IEProperties.FACING_HORIZONTAL;
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(state, level, pos, neighbor);
        BlockEntity tile = level.getBlockEntity(pos);
        if(tile instanceof SchematicBoardBlockEntity board)
        {
            board.updateEdges(pos);
            board.updateEdges(neighbor);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        // Boards keep their pinned schematics when broken, so put them back when placed again
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof SchematicBoardBlockEntity board)
            board.restorePinned(stack);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean pMovedByPiston) {
        super.onPlace(state, level, pos, oldState, pMovedByPiston);
        BlockEntity tile = level.getBlockEntity(pos);
        if(tile instanceof SchematicBoardBlockEntity board)
        {
            board.onInitialPlace();
        }
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return ESRegistry.SCHEMATIC_BOARD_TYPE.get().create(pos, state);
    }
}
