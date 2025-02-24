package tech.muddykat.engineered_schematics.registry;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL11;
import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.client.ESShaders;

public class ESRenderTypes extends RenderStateShard {
    public ESRenderTypes(String pName, Runnable pSetupState, Runnable pClearState) {
        super(pName, pSetupState, pClearState);
    }

    static final RenderStateShard.DepthTestStateShard DEPTH_ALWAYS = new RenderStateShard.DepthTestStateShard("greater", GL11.GL_LESS);
    public static final RenderType SCHEMATIC;

    static final RenderStateShard.ShaderStateShard BLUEPRINT_SHADER = new RenderStateShard.ShaderStateShard(ESShaders::getSchematicShader);
    static
    {
        SCHEMATIC = RenderType.create(
                typeName("rendertype_blueprint"),
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                RenderType.BIG_BUFFER_SIZE,
                true,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(BLUEPRINT_SHADER)
                        .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setOutputState(TRANSLUCENT_TARGET)
                        .setDepthTestState(DEPTH_ALWAYS)
                        .createCompositeState(false)
        );
    }

    private static String typeName(String str){
        return EngineeredSchematics.MODID + ":" + str;
    }
}
