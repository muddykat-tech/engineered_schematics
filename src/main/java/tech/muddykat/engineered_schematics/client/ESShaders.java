package tech.muddykat.engineered_schematics.client;


import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import tech.muddykat.engineered_schematics.EngineeredSchematics;

import java.io.IOException;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid =  EngineeredSchematics.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ESShaders {
    private static ShaderInstance shader_schematic;


    private static AbstractUniform projection_time;
    private static AbstractUniform projection_grid;
    private static AbstractUniform color_tint;

    public static void setSchematicRenderData(float time, float red, float green, float blue)
    {
        ESShaders.projection_time.set(time);
        ESShaders.projection_grid.set(0.9f);
        ESShaders.color_tint.set(red, green, blue);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException
    {
        ShaderInstance instance = new ShaderInstance(event.getResourceProvider(), new ResourceLocation(EngineeredSchematics.MODID,"rendertype_schematic"), DefaultVertexFormat.POSITION_COLOR_TEX);

        event.registerShader(instance,  s -> {
            shader_schematic = s;

            projection_time = shader_schematic.safeGetUniform("Time");
            projection_grid = shader_schematic.safeGetUniform("GridThickness");
            color_tint = shader_schematic.safeGetUniform("ColorTint");
        });
    }

    public static ShaderInstance getSchematicShader()
    {
        return shader_schematic;
    }
}