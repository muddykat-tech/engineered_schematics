package tech.muddykat.engineered_schematics.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.neoforge.registries.DeferredRegister;
import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.item.SchematicProjectionData;

import java.util.function.BiConsumer;

public class ESDataComponents {

    public static final ResourceKey<Registry<DataComponentType<?>>> DATA_COMPONENT_TYPE_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(EngineeredSchematics.MODID, "data_component_types"));

    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.DataComponents.createDataComponents(DATA_COMPONENT_TYPE_KEY, EngineeredSchematics.MODID);

    // Register schematic projection data component
    public static final DataComponentType<SchematicProjectionData> SCHEMATIC_PROJECTION_DATA =
            DataComponentType.<SchematicProjectionData>builder()
                    .persistent(SchematicProjectionData.CODEC)
                    .networkSynchronized(SchematicProjectionData.STREAM_CODEC)
                    .build();

    // Register the DataComponentType
    public static void registerComponents() {
        DATA_COMPONENTS.register("schematic_projection_data", ()->SCHEMATIC_PROJECTION_DATA);
    }
}
