package tech.muddykat.engineered_schematics.helper;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.register.IEMenuTypes;
import com.google.common.base.Preconditions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import tech.muddykat.engineered_schematics.registry.ESMenuTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IInteractionObjectES<T extends BlockEntity & IInteractionObjectES<T>> extends MenuProvider {
    @Nullable
    T getGuiMaster();

    ESMenuTypes.ArgContainer<? super T, ?> getContainerType();

    boolean canUseGui(Player var1);

    default boolean isValid() {
        return this.getGuiMaster() != null;
    }

    @Nonnull
    default AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player playerEntity) {
        T master = this.getGuiMaster();
        Preconditions.checkNotNull(master);
        ESMenuTypes.ArgContainer<? super T, ?> type = this.getContainerType();
        return type.create(id, playerInventory, master);
    }

    default Component getDisplayName() {
        return Component.literal("");
    }
}
