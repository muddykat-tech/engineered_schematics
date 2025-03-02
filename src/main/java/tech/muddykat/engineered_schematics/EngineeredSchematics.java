package tech.muddykat.engineered_schematics;

import blusunrize.immersiveengineering.api.ManualHelper;
import blusunrize.immersiveengineering.api.client.ieobj.IEOBJCallbacks;
import blusunrize.immersiveengineering.common.blocks.wooden.BlueprintShelfBlock;
import blusunrize.immersiveengineering.common.blocks.wooden.BlueprintShelfBlockEntity;
import blusunrize.lib.manual.ManualEntry;
import blusunrize.lib.manual.ManualInstance;
import blusunrize.lib.manual.Tree;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import tech.muddykat.engineered_schematics.event.SchematicPickBlockHandler;
import tech.muddykat.engineered_schematics.helper.SchematicTableCallbacks;
import tech.muddykat.engineered_schematics.client.screen.SchematicsScreen;
import tech.muddykat.engineered_schematics.registry.ESMenuTypes;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import static tech.muddykat.engineered_schematics.registry.ESRegistry.BLOCK_ITEM_SCHEMATIC_TABLE;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(EngineeredSchematics.MODID)
public class EngineeredSchematics
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "engineered_schematics";
    public static final String SCHEMATIC_GUIID = "schematic_table";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public EngineeredSchematics(IEventBus modEventBus)
    {
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
            NeoForge.EVENT_BUS.register(new SchematicPickBlockHandler());
            setupManualEntries();
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
