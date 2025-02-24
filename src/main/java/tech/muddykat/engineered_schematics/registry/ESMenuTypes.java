package tech.muddykat.engineered_schematics.registry;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.common.gui.IEBaseContainerOld;
import blusunrize.immersiveengineering.common.gui.IEContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.block.entity.SchematicTableBlockEntity;
import tech.muddykat.engineered_schematics.menu.SchematicsContainerMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ESMenuTypes {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(BuiltInRegistries.MENU, EngineeredSchematics.MODID);
    public static void register(IEventBus bus)
    {
        REGISTER.register(bus);
    }

    public static final ArgContainer<SchematicTableBlockEntity, SchematicsContainerMenu> SCHEMATICS = register(EngineeredSchematics.SCHEMATIC_GUIID, SchematicsContainerMenu::new);

    public static <T extends BlockEntity, C extends IEBaseContainerOld<? super T>>
    ArgContainer<T, C> register(String name, ArgContainerConstructor<T, C> container)
    {
        DeferredHolder<MenuType<?>, MenuType<C>> typeRef = REGISTER.register(
                name, () -> {
                    Mutable<MenuType<C>> typeBox = new MutableObject<>();
                    MenuType<C> type = new MenuType<>((IContainerFactory<C>)(windowId, inv, data) -> {
                        Level world = ImmersiveEngineering.proxy.getClientWorld();
                        BlockPos pos = data.readBlockPos();
                        BlockEntity te = world.getBlockEntity(pos);
                        return container.construct(typeBox.getValue(), windowId, inv, (T)te);
                    }, FeatureFlagSet.of());
                    typeBox.setValue(type);
                    return type;
                }
        );
        return new ArgContainer<>(typeRef, container);
    }


    public static class ArgContainer<T, C extends IEContainerMenu>
    {
        private final DeferredHolder<MenuType<?>, MenuType<C>> type;
        private final ArgContainerConstructor<T, C> factory;

        private ArgContainer(DeferredHolder<MenuType<?>, MenuType<C>> type, ArgContainerConstructor<T, C> factory)
        {
            this.type = type;
            this.factory = factory;
        }

        public C create(int windowId, Inventory playerInv, T tile)
        {
            return factory.construct(getType(), windowId, playerInv, tile);
        }

        public MenuProvider provide(T arg)
        {
            return new MenuProvider()
            {
                @Nonnull
                @Override
                public Component getDisplayName()
                {
                    return Component.empty();
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(
                        int containerId, @Nonnull Inventory inventory, @Nonnull Player player
                )
                {
                    return create(containerId, inventory, arg);
                }
            };
        }

        public MenuType<C> getType()
        {
            return type.get();
        }
    }

    public interface ArgContainerConstructor<T, C extends IEContainerMenu>
    {
        C construct(MenuType<C> type, int windowId, Inventory inventoryPlayer, T te);
    }
}
