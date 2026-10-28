package tech.muddykat.engineered_schematics.item;

import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.block.SchematicDeskBlock;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Objects;

public class SchematicTableBlockItem extends ItemBlock {
    public SchematicTableBlockItem(Block block) {
        super(block);
        setRegistryName(Objects.requireNonNull(block.getRegistryName()));
        setTranslationKey(EngineeredSchematics.MODID + ".schematic_table_block");
        setMaxStackSize(1);
        setCreativeTab(EngineeredSchematics.CREATIVE_TAB);
    }

    @Override public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull IBlockState newState) {
        BlockPos dummyPos = pos.offset(SchematicDeskBlock.partnerDirection(newState));
        if (!player.canPlayerEdit(dummyPos, side, stack) || !world.mayPlace(this.block, dummyPos, false, side, player)) { return false; }
        return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
    }
}
