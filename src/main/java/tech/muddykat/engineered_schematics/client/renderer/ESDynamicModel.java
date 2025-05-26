package tech.muddykat.engineered_schematics.client.renderer;

import blusunrize.immersiveengineering.api.ApiUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import tech.muddykat.engineered_schematics.EngineeredSchematics;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = EngineeredSchematics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ESDynamicModel {
    private static final List<ModelResourceLocation> MODELS = new ArrayList<>();

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional ev)
    {
        for(ModelResourceLocation model : MODELS)
            ev.register(model);
    }

    private final ModelResourceLocation name;

    public ESDynamicModel(String desc)
    {
        // References a generated json file
        this.name = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(EngineeredSchematics.MODID, "dynamic/"+desc));
        MODELS.add(this.name);
    }

    public BakedModel get()
    {
        final BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        return blockRenderer.getBlockModelShaper().getModelManager().getModel(name);
    }

    public List<BakedQuad> getNullQuads(ModelData data)
    {
        return get().getQuads(null, null, ApiUtils.RANDOM_SOURCE, data, null);
    }

    public ModelResourceLocation getName()
    {
        return name;
    }
}
