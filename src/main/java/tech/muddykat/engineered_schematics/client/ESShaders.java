package tech.muddykat.engineered_schematics.client;

import tech.muddykat.engineered_schematics.EngineeredSchematics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.client.shader.ShaderManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;

@SideOnly(Side.CLIENT)
public class ESShaders {
    private static final String PROGRAM = EngineeredSchematics.MODID + ":schematic";
    private static final String[] OPTIFINE_SHADERS = {"net.optifine.shaders.Shaders", "shadersmod.client.Shaders"};
    private static final float FALLBACK_ALPHA = 0.6F;
    private static final float FALLBACK_TINT = 0.3F;
    private static ShaderManager schematic;
    private static boolean unavailable;
    private static boolean shaderActive;
    private static boolean fallbackActive;
    private static Field shaderPackLoaded;
    private static boolean optifineChecked;

    public static void use(float time, float red, float green, float blue) {
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.depthFunc(GL11.GL_LESS);
        ShaderManager shader = isShaderPackActive() ? null : getSchematicShader();
        if (shader == null) {
            // Vertex colours are fixed by the model renderer, so the tint and fade come from the constant blend colour
            GL14.glBlendColor(mixTint(red) * FALLBACK_ALPHA, mixTint(green) * FALLBACK_ALPHA, mixTint(blue) * FALLBACK_ALPHA, FALLBACK_ALPHA);
            GlStateManager.blendFunc(GL11.GL_CONSTANT_COLOR, GL11.GL_ONE_MINUS_CONSTANT_ALPHA);
            fallbackActive = true;
            return;
        }
        shader.getShaderUniformOrDefault("Time").set(time);
        shader.getShaderUniformOrDefault("ColorTint").set(red, green, blue);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        shader.useShader();
        shaderActive = true;
    }

    public static void end() {
        if (shaderActive) {
            schematic.endShader();
            shaderActive = false;
        }
        if (fallbackActive) {
            GL14.glBlendColor(0.0F, 0.0F, 0.0F, 0.0F);
            fallbackActive = false;
        }
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
    }

    private static float mixTint(float channel) { return 1.0F - FALLBACK_TINT + FALLBACK_TINT * channel; }

    /** OptiFine shader packs render into their own buffers, which our program would bypass and leave the projection invisible. */
    public static boolean isShaderPackActive() {
        if (!optifineChecked) {
            optifineChecked = true;
            for (String name : OPTIFINE_SHADERS) {
                try {
                    shaderPackLoaded = Class.forName(name).getField("shaderPackLoaded");
                    EngineeredSchematics.LOGGER.info("Found OptiFine shaders, the schematic projection will skip its own shader while a shader pack is loaded");
                    break;
                }
                catch (ReflectiveOperationException | LinkageError ignored) {}
            }
        }
        if (shaderPackLoaded == null) { return false; }
        try { return shaderPackLoaded.getBoolean(null); }
        catch (IllegalAccessException exception) { return false; }
    }

    @Nullable
    @SuppressWarnings("ConstantConditions")
    public static ShaderManager getSchematicShader() {
        if (schematic != null || unavailable) { return schematic; }
        if (!OpenGlHelper.areShadersSupported()) {
            unavailable = true;
            EngineeredSchematics.LOGGER.warn("Shaders are not supported, the schematic projection will use a plain tint");
            return null;
        }
        if (ShaderLinkHelper.getStaticShaderLinkHelper() == null) { ShaderLinkHelper.setNewStaticShaderLinkHelper(); }
        try {
            schematic = new ShaderManager(Minecraft.getMinecraft().getResourceManager(), PROGRAM);
            schematic.addSamplerTexture("Sampler0", Minecraft.getMinecraft().getTextureMapBlocks());
        }
        catch (IOException exception) {
            unavailable = true;
            EngineeredSchematics.LOGGER.error("Could not load the schematic shader program " + PROGRAM, exception);
        }
        return schematic;
    }
}
