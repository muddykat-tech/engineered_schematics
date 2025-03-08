package tech.muddykat.engineered_schematics.block;

import blusunrize.immersiveengineering.common.blocks.wooden.DeskBlock;
import blusunrize.immersiveengineering.common.gui.IEBaseContainerOld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.RegistryObject;
import tech.muddykat.engineered_schematics.block.entity.SchematicTableBlockEntity;
import tech.muddykat.engineered_schematics.helper.IInteractionObjectES;

import java.util.function.Supplier;

public class SchematicDeskBlock<T extends BlockEntity> extends DeskBlock<T> {
    public SchematicDeskBlock(RegistryObject<BlockEntityType<T>> tileType, Properties props) {
        super(tileType, props);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        BlockEntity tile = world.getBlockEntity(pos);
        InteractionResult superResult = InteractionResult.SUCCESS;
        if(tile instanceof MenuProvider menuProvider&&hand==InteractionHand.MAIN_HAND&&!player.isShiftKeyDown())
        {
            if(player instanceof ServerPlayer serverPlayer)
            {
                if(menuProvider instanceof IInteractionObjectES<?> interaction)
                {
                    interaction = interaction.getGuiMaster();
                    if(interaction!=null&&interaction.canUseGui(player))
                    {
                        // This can be removed once IEBaseContainerOld is gone
                        var tempMenu = interaction.createMenu(0, player.getInventory(), player);
                        if(tempMenu instanceof IEBaseContainerOld<?>)
                            NetworkHooks.openScreen(serverPlayer, interaction, ((BlockEntity)interaction).getBlockPos());
                        else
                            NetworkHooks.openScreen(serverPlayer, interaction);
                    }
                }
                else
                    NetworkHooks.openScreen(serverPlayer, menuProvider);
            }
            return InteractionResult.SUCCESS;
        }
        return superResult;
    }

}
