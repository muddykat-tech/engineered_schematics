package tech.muddykat.engineered_schematics.registry;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IEMultiblocks;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.block.SchematicDeskBlock;
import tech.muddykat.engineered_schematics.block.entity.SchematicTableBlockEntity;
import tech.muddykat.engineered_schematics.item.ESSchematicSettings;
import tech.muddykat.engineered_schematics.item.SchematicItem;
import tech.muddykat.engineered_schematics.item.SchematicTableBlockItem;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ESRegistry {

    private static final DeferredRegister<BlockEntityType<?>> TE_REGISTER = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EngineeredSchematics.MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EngineeredSchematics.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EngineeredSchematics.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EngineeredSchematics.MODID);


    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<SchematicTableBlockEntity>> SCHEMATIC_TABLE_TYPE;
    public static DeferredBlock<Block> BLOCK_SCHEMATIC_TABLE;
    public static DeferredItem<Item> SCHEMATIC_ITEM;
    public static DeferredItem<BlockItem> BLOCK_ITEM_SCHEMATIC_TABLE;

    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register(EngineeredSchematics.MODID, () -> // Add the example item to the tab. For your own tabs, this method is preferred over the event
            CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.engineered_schematics")) //The language key for the title of your CreativeModeTab
            .icon(() -> BLOCK_ITEM_SCHEMATIC_TABLE.get().getDefaultInstance())
            .displayItems(ESRegistry::getDisplayItems).build());

    public static void register(IEventBus bus)
    {
        ITEMS.register(bus);
        BLOCKS.register(bus);
        TE_REGISTER.register(bus);
        CREATIVE_MODE_TABS.register(bus);
    }

    public static void getDisplayItems(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output)
    {
        output.accept(BLOCK_ITEM_SCHEMATIC_TABLE.get());
        for(MultiblockHandler.IMultiblock mb : IEMultiblocks.IE_MULTIBLOCKS)
        {
            ItemStack blueprint = new ItemStack(SCHEMATIC_ITEM.get());
            ESSchematicSettings settings = new ESSchematicSettings(blueprint);
            settings.setMultiblock(mb);
            settings.setMirror(false);
            settings.setRotation(Rotation.NONE);
            settings.setPlaced(false);
            settings.applyTo(blueprint);
            output.accept(blueprint);
        }
    }

    public static void initialize()
    {
        SCHEMATIC_ITEM = ITEMS.register("multiblock_schematic", SchematicItem::new);
        BLOCK_SCHEMATIC_TABLE = BLOCKS.register("schematic_table_block", () -> new SchematicDeskBlock<>(SCHEMATIC_TABLE_TYPE, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).ignitedByLava().instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).strength(2.0F, 5.0F).noOcclusion()));
        SCHEMATIC_TABLE_TYPE = TE_REGISTER.register("schematic_table_type", makeType(SchematicTableBlockEntity::new, BLOCK_SCHEMATIC_TABLE));
        BLOCK_ITEM_SCHEMATIC_TABLE = ITEMS.register("schematic_table_block", () -> new SchematicTableBlockItem(BLOCK_SCHEMATIC_TABLE.get()));
    }

    public static <T extends BlockEntity> Supplier<BlockEntityType<T>> makeType(BlockEntityType.BlockEntitySupplier<T> create, Supplier<? extends Block> valid)
    {
        return makeTypeMultipleBlocks(create, ImmutableSet.of(valid));
    }

    public static <T extends BlockEntity> Supplier<BlockEntityType<T>> makeTypeMultipleBlocks(
            BlockEntityType.BlockEntitySupplier<T> create, Collection<? extends Supplier<? extends Block>> valid
    )
    {
        return () -> new BlockEntityType<>(
                create, ImmutableSet.copyOf(valid.stream().map(Supplier::get).collect(Collectors.toList())), null
        );
    }

}
