package tech.muddykat.engineered_schematics.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import tech.muddykat.engineered_schematics.block.SchematicDeskBlock;

public class SchematicTableBlockItem extends BlockItem {
    public SchematicTableBlockItem(Block pBlock) {
        super(pBlock, new Properties().stacksTo(1));
    }

    @Override
    protected boolean placeBlock(@NotNull BlockPlaceContext context, @NotNull BlockState state)
    {
        Block b = state.getBlock();
        if(b instanceof SchematicDeskBlock<?> desk)
        {
            boolean ret = super.placeBlock(context, state);
            if(ret) desk.onIEBlockPlacedBy(context, state);
            return ret;
        }
        return super.placeBlock(context, state);
    }
}
