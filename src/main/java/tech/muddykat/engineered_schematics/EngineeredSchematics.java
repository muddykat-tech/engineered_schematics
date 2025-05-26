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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;
import tech.muddykat.engineered_schematics.client.renderer.CorkboardRenderer;
import tech.muddykat.engineered_schematics.client.renderer.ESDynamicModel;
import tech.muddykat.engineered_schematics.event.SchematicPickBlockHandler;
import tech.muddykat.engineered_schematics.helper.SchematicTableCallbacks;
import tech.muddykat.engineered_schematics.client.screen.SchematicsScreen;
import tech.muddykat.engineered_schematics.registry.ESMenuTypes;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import java.util.HashMap;
import java.util.function.Supplier;
import static tech.muddykat.engineered_schematics.registry.ESRegistry.BLOCK_ITEM_SCHEMATIC_TABLE;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(EngineeredSchematics.MODID)
public class EngineeredSchematics
{
    public static final String MODID = "engineered_schematics";
    public static final String SCHEMATIC_GUIID = "schematic_table";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final HashMap<ResourceLocation, ItemStack> ES_FORMATION_TEMPLATE = new HashMap<>();

    public EngineeredSchematics(IEventBus modEventBus, ModContainer modContainer)
    {
        LogUtils.getLogger().info("Starting Engineered Schematics");

        // Register the commonSetup method for modloading
        ESRegistry.register(modEventBus);
        ESMenuTypes.register(modEventBus);
        ESRegistry.initialize();
        modEventBus.addListener(this::addCreative);

        if(FMLLoader.getDist().isClient())
        {
            setupCallbacks();
        }
    }
    public static void setupCallbacks()
    {
        IEOBJCallbacks.register(rl("schematic_table_block"), SchematicTableCallbacks.INSTANCE);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static void setTemplateFormationItem(ResourceLocation id, ItemStack item)
    {
        ES_FORMATION_TEMPLATE.replace(id, item);
    }

    public static boolean hasFormationItem(ResourceLocation uniqueName) {
        return ES_FORMATION_TEMPLATE.containsKey(uniqueName);
    }

    public static ItemStack getFormationItem(ResourceLocation uniqueName)
    {
        return ES_FORMATION_TEMPLATE.get(uniqueName);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(BLOCK_ITEM_SCHEMATIC_TABLE.get());
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CommonModEvents
    {
        @SubscribeEvent
        public static void registerContainersAndScreens(RegisterMenuScreensEvent event)
        {
            event.register(ESMenuTypes.SCHEMATICS.getType(), SchematicsScreen::new);
        }

    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            NeoForge.EVENT_BUS.register(new SchematicPickBlockHandler());
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
            Tree.InnerNode<ResourceLocation, ManualEntry> parent_category = instance.getRoot().getOrCreateSubnode(ResourceLocation.fromNamespaceAndPath(EngineeredSchematics.MODID, "main"), 99);

            ManualEntry.ManualEntryBuilder builder = new ManualEntry.ManualEntryBuilder(ManualHelper.getManual());
            builder.readFromFile(ResourceLocation.fromNamespaceAndPath(EngineeredSchematics.MODID, "es"));
            instance.addEntry(parent_category, builder.create());

            builder.readFromFile(ResourceLocation.fromNamespaceAndPath(EngineeredSchematics.MODID, "schematic_table"));
            instance.addEntry(parent_category, builder.create());

            builder.readFromFile(ResourceLocation.fromNamespaceAndPath(EngineeredSchematics.MODID, "schematic_item"));
            instance.addEntry(parent_category, builder.create());
        }
    }

    public static ResourceLocation makeTextureLocation(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID,"textures/gui/" + name + ".png");
    }
}
