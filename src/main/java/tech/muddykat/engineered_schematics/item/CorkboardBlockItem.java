package tech.muddykat.engineered_schematics.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import tech.muddykat.engineered_schematics.block.SchematicCorkBoard;
import tech.muddykat.engineered_schematics.block.SchematicDeskBlock;

public class CorkboardBlockItem extends BlockItem {
    public CorkboardBlockItem(Block pBlock) {
        super(pBlock, new Properties());
    }

}
