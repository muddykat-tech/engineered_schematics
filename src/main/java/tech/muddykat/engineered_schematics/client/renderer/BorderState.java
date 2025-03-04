package tech.muddykat.engineered_schematics.client.renderer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.Map;

public class BorderState {
    // Edges
    private boolean topEdge = true;
    private boolean rightEdge = true;
    private boolean bottomEdge = true;
    private boolean leftEdge = true;

    // Corners
    private boolean topRightCorner = true;
    private boolean bottomRightCorner = true;
    private boolean bottomLeftCorner = true;
    private boolean topLeftCorner = true;

    // Getters
    public boolean hasTopEdge() { return topEdge; }
    public boolean hasRightEdge() { return rightEdge; }
    public boolean hasBottomEdge() { return bottomEdge; }
    public boolean hasLeftEdge() { return leftEdge; }

    public boolean hasTopRightCorner() { return topRightCorner; }
    public boolean hasBottomRightCorner() { return bottomRightCorner; }
    public boolean hasBottomLeftCorner() { return bottomLeftCorner; }
    public boolean hasTopLeftCorner() { return topLeftCorner; }

    /**
     * Updates a specific edge based on direction
     * @param dir The direction of the edge
     * @param visible Whether the edge should be visible
     * @param facing The block's facing direction
     */
    public void updateEdge(Direction dir, boolean visible, Direction facing) {
        if(dir == Direction.UP) topEdge = visible;
        else if(dir == Direction.DOWN) bottomEdge = visible;
        else if(dir == facing.getClockWise()) rightEdge = visible;
        else if(dir == facing.getCounterClockWise()) leftEdge = visible;
    }

    public void updateCorners(Map<Direction, Boolean> adjacent, Map<BlockPos, Boolean> diagonalBlocks, Direction facing, BlockPos pos) {
        Level level = null; // You'll need to provide the level when calling this method

        // Top-right corner
        boolean hasDiagonalTopRight = diagonalBlocks.getOrDefault(pos.relative(Direction.UP).relative(facing.getClockWise()), false);
        topRightCorner = !(adjacent.getOrDefault(Direction.UP, false) &&
                adjacent.getOrDefault(facing.getClockWise(), false) &&
                hasDiagonalTopRight);

        // Bottom-right corner
        boolean hasDiagonalBottomRight = diagonalBlocks.getOrDefault(pos.relative(Direction.DOWN).relative(facing.getClockWise()), false);
        bottomRightCorner = !(adjacent.getOrDefault(Direction.DOWN, false) &&
                adjacent.getOrDefault(facing.getClockWise(), false) &&
                hasDiagonalBottomRight);

        // Bottom-left corner
        boolean hasDiagonalBottomLeft = diagonalBlocks.getOrDefault(pos.relative(Direction.DOWN).relative(facing.getCounterClockWise()), false);
        bottomLeftCorner = !(adjacent.getOrDefault(Direction.DOWN, false) &&
                adjacent.getOrDefault(facing.getCounterClockWise(), false) &&
                hasDiagonalBottomLeft);

        // Top-left corner
        boolean hasDiagonalTopLeft = diagonalBlocks.getOrDefault(pos.relative(Direction.UP).relative(facing.getCounterClockWise()), false);
        topLeftCorner = !(adjacent.getOrDefault(Direction.UP, false) &&
                adjacent.getOrDefault(facing.getCounterClockWise(), false) &&
                hasDiagonalTopLeft);
    }

    public void updateFromNBT(CompoundTag nbt) {
        topEdge = nbt.getBoolean("topEdge");
        rightEdge = nbt.getBoolean("rightEdge");
        bottomEdge = nbt.getBoolean("bottomEdge");
        leftEdge = nbt.getBoolean("leftEdge");

        topRightCorner = nbt.getBoolean("topRightCorner");
        bottomRightCorner = nbt.getBoolean("bottomRightCorner");
        bottomLeftCorner = nbt.getBoolean("bottomLeftCorner");
        topLeftCorner = nbt.getBoolean("topLeftCorner");
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putBoolean("topEdge",topEdge);
        nbt.putBoolean("rightEdge",rightEdge);
        nbt.putBoolean("bottomEdge",bottomEdge);
        nbt.putBoolean("leftEdge",leftEdge);

        nbt.putBoolean("topRightCorner",topRightCorner);
        nbt.putBoolean("bottomRightCorner",bottomRightCorner);
        nbt.putBoolean("bottomLeftCorner",bottomLeftCorner);
        nbt.putBoolean("topLeftCorner",topLeftCorner);
    }
}
