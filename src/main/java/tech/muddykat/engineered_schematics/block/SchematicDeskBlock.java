package tech.muddykat.engineered_schematics.block;

import blusunrize.immersiveengineering.common.blocks.wooden.DeskBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SchematicDeskBlock<T extends BlockEntity> extends DeskBlock<T> {
    public SchematicDeskBlock(DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> tileType, Properties props) {
        super(tileType, props);
    }
}
