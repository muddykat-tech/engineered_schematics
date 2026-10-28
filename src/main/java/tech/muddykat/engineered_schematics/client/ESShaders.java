package tech.muddykat.engineered_schematics.client;


import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tech.muddykat.engineered_schematics.EngineeredSchematics;

import java.io.IOException;
import java.lang.reflect.Method;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid =  EngineeredSchematics.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ESShaders {
    private static ShaderInstance shader_schematic;
    private static AbstractUniform projection_time;
    private static AbstractUniform color_tint;

    private static final String IRIS_API = "net.irisshaders.iris.api.v0.IrisApi";
    private static Object irisApi;
    private static Method shaderPackInUse;
    private static boolean irisChecked;

    public static void setSchematicRenderData(float time, float red, float green, float blue)
    {
        if(shader_schematic == null) return;
        ESShaders.projection_time.set(time);
        ESShaders.color_tint.set(red, green, blue);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException
    {
        ShaderInstance instance = new ShaderInstance(event.getResourceProvider(), new ResourceLocation(EngineeredSchematics.MODID,"rendertype_schematic"), DefaultVertexFormat.POSITION_COLOR_TEX);

        event.registerShader(instance,  s -> {
            shader_schematic = s;
            projection_time = shader_schematic.safeGetUniform("Time");
            color_tint = shader_schematic.safeGetUniform("ColorTint");
        });
    }

    public static ShaderInstance getSchematicShader()
    {
        return shader_schematic;
    }

    /** Oculus/Iris shader packs replace the pipeline our shader runs in, which leaves the projection invisible. */
    public static boolean isShaderPackActive()
    {
        if(!irisChecked)
        {
            irisChecked = true;
            try
            {
                Class<?> api = Class.forName(IRIS_API);
                irisApi = api.getMethod("getInstance").invoke(null);
                shaderPackInUse = api.getMethod("isShaderPackInUse");
            }
            catch(ReflectiveOperationException|LinkageError ignored) {}
        }
        if(shaderPackInUse == null) return false;
        try
        {
            return (Boolean)shaderPackInUse.invoke(irisApi);
        }
        catch(ReflectiveOperationException e)
        {
            return false;
        }
    }

    /** Whether the projection can use the schematic shader rather than the vanilla fallback. */
    public static boolean useSchematicShader()
    {
        return shader_schematic != null && !isShaderPackActive();
    }
}