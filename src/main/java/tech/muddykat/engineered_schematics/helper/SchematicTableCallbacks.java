package tech.muddykat.engineered_schematics.helper;

import blusunrize.immersiveengineering.api.IEProperties.*;
import blusunrize.immersiveengineering.api.client.ieobj.BlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import tech.muddykat.engineered_schematics.block.entity.SchematicTableBlockEntity;

public class SchematicTableCallbacks implements BlockCallback<Integer>
{
    public static final SchematicTableCallbacks INSTANCE = new SchematicTableCallbacks();

    @Override
    public Integer extractKey(@NotNull BlockAndTintGetter blockAndTintGetter, @NotNull BlockPos blockPos, @NotNull BlockState blockState, BlockEntity blockEntity)
    {
        if(!(blockEntity instanceof SchematicTableBlockEntity table))
            return 0;

        SchematicTableBlockEntity master = table.master();
        if(master == null) return 0;

        NonNullList<ItemStack> inv = master.getInventory();
        if(inv.get(0).getCount() > 32) return 3;
        if(inv.get(0).getCount() > 15) return 2;
        if(inv.get(0).getCount() > 0) return 1;

        return 0;
    }

    @Override
    public Integer getDefaultKey()
    {
        return 0;
    }

    private static final IEObjState no_paper = new IEObjState(VisibilityList.show("base_model"));
    private static final IEObjState some_paper = new IEObjState(VisibilityList.show("base_model", "scroll_1"));
    private static final IEObjState paper = new IEObjState(VisibilityList.show("base_model", "scroll_1", "scroll_2"));
    private static final IEObjState much_paper = new IEObjState(VisibilityList.show("base_model", "scroll_1", "scroll_2", "scroll_3"));

    @Override
    public IEObjState getIEOBJState(Integer paper_level)
    {
        if(paper_level == 1) return some_paper;
        if(paper_level == 2) return paper;
        if(paper_level == 3) return much_paper;
        return no_paper;
    }
}
