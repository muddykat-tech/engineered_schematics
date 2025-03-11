package tech.muddykat.engineered_schematics;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.immersiveengineering.api.client.ieobj.IEOBJCallbacks;
import blusunrize.lib.manual.ManualEntry;
import blusunrize.lib.manual.ManualInstance;
import blusunrize.lib.manual.Tree;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import tech.muddykat.engineered_schematics.client.renderer.CorkboardRenderer;
import tech.muddykat.engineered_schematics.client.renderer.ESDynamicModel;
import tech.muddykat.engineered_schematics.event.SchematicPickBlockHandler;
import tech.muddykat.engineered_schematics.helper.SchematicTableCallbacks;
import tech.muddykat.engineered_schematics.client.screen.SchematicsScreen;
import tech.muddykat.engineered_schematics.registry.ESMenuTypes;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import java.util.function.Supplier;

import static tech.muddykat.engineered_schematics.registry.ESRegistry.BLOCK_ITEM_SCHEMATIC_TABLE;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(EngineeredSchematics.MODID)
public class EngineeredSchematics
{
    public static final String MODID = "engineered_schematics";
    public static final String SCHEMATIC_GUIID = "schematic_table";
    public EngineeredSchematics(IEventBus modEventBus)
    {
        LogUtils.getLogger().info("Starting Engineered Schematics");
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        ESRegistry.register(modEventBus);
        ESMenuTypes.register(modEventBus);

        ESRegistry.initialize();

        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(BLOCK_ITEM_SCHEMATIC_TABLE);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            IEOBJCallbacks.register(new ResourceLocation(MODID, "schematic_table_block"), SchematicTableCallbacks.INSTANCE);
            MenuScreens.register(ESMenuTypes.SCHEMATICS.getType(), SchematicsScreen::new);
            MinecraftForge.EVENT_BUS.register(new SchematicPickBlockHandler());
            setupManualEntries();
        }

        @SubscribeEvent
        public static void registerModelLoaders(ModelEvent.RegisterGeometryLoaders event)
        {
            CorkboardRenderer.FRAME_EDGE = new ESDynamicModel("frame_edge");
            CorkboardRenderer.CORNER = new ESDynamicModel("corner");
            CorkboardRenderer.SCHEMATIC = new ESDynamicModel("schematic");
        }

        private static <T extends BlockEntity>
        void registerBERenderNoContext(
                EntityRenderersEvent.RegisterRenderers event, Supplier<BlockEntityType<? extends T>> type, Supplier<BlockEntityRenderer<T>> render
        )
        {
            registerBERenderNoContext(event, type.get(), render);
        }

        private static <T extends BlockEntity>
        void registerBERenderNoContext(
                EntityRenderersEvent.RegisterRenderers event, BlockEntityType<? extends T> type, Supplier<BlockEntityRenderer<T>> render
        )
        {
            event.registerBlockEntityRenderer(type, $ -> render.get());
        }


        @SubscribeEvent
        public static void registerBERenderer(EntityRenderersEvent.RegisterRenderers event)
        {
            registerBERenderNoContext(event, ESRegistry.SCHEMATIC_BOARD_TYPE.get(), CorkboardRenderer::new);
        }

        private static void setupManualEntries()
        {
            ManualInstance instance = ManualHelper.getManual();
            Tree.InnerNode<ResourceLocation, ManualEntry> parent_category = instance.getRoot().getOrCreateSubnode(new ResourceLocation(EngineeredSchematics.MODID, "main"), 99);

            ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(ManualHelper.getManual());
            builder.readFromFile(new ResourceLocation(EngineeredSchematics.MODID, "es"));
            instance.addEntry(parent_category, builder.create());

            builder.readFromFile(new ResourceLocation(EngineeredSchematics.MODID, "schematic_table"));
            instance.addEntry(parent_category, builder.create());

            builder.readFromFile(new ResourceLocation(EngineeredSchematics.MODID, "schematic_item"));
            instance.addEntry(parent_category, builder.create());
        }
    }

    public static ResourceLocation makeTextureLocation(String name) {
        return new ResourceLocation(MODID,"textures/gui/" + name + ".png");
    }
}
