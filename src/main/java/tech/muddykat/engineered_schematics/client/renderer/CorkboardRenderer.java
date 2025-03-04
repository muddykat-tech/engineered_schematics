package tech.muddykat.engineered_schematics.client.renderer;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.utils.DirectionUtils;
import blusunrize.immersiveengineering.client.utils.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.data.models.ModelProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;
import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.block.entity.SchematicBoardBlockEntity;

import java.util.*;

import static tech.muddykat.engineered_schematics.block.entity.SchematicBoardBlockEntity.getTargetedSlot;

public class CorkboardRenderer implements BlockEntityRenderer<SchematicBoardBlockEntity> {

    private static final Map<String, BakedModel> modelCache = new HashMap<>();
    public static ESDynamicModel FRAME_EDGE;
    public static ESDynamicModel CORNER;
    public static ESDynamicModel SCHEMATIC;
    private static final Map<Direction, Quaternionf> ROTATE_FOR_FACING = Util.make(
            new EnumMap<>(Direction.class), m -> {
                for (Direction facing : DirectionUtils.BY_HORIZONTAL_INDEX)
                    m.put(facing, new Quaternionf().rotateY(Mth.DEG_TO_RAD * (180 - facing.toYRot())));
            }
    );

    protected static void rotateForFacingNoCentering(PoseStack stack, Direction facing) {
        stack.mulPose(ROTATE_FOR_FACING.get(facing));
    }

    protected static void rotateForFacing(PoseStack stack, Direction facing) {
        stack.translate(0.5, 0.5, 0.5);
        rotateForFacingNoCentering(stack, facing);
        stack.translate(-0.5, -0.5, -0.5);
    }


    private void renderCornerBL(PoseStack pose, MultiBufferSource mainBuffer, Level level, BlockPos pos, int pPackedLight, int pPackedOverlay)
    {
        pose.pushPose();
        {
            pose.translate(0, -0.9375f, 0);
            renderDynamicModel(CORNER, pose, mainBuffer, Direction.NORTH, level, pos, pPackedLight, pPackedOverlay);
        }
        pose.popPose();
    }


    private void renderCornerTL(PoseStack pose, MultiBufferSource mainBuffer, Level level, BlockPos pos, int pPackedLight, int pPackedOverlay)
    {
        pose.pushPose();
        {
            renderDynamicModel(CORNER, pose, mainBuffer, Direction.NORTH, level, pos, pPackedLight, pPackedOverlay);
        }
        pose.popPose();
    }
    private void renderCornerTR(PoseStack pose, MultiBufferSource mainBuffer, Level level, BlockPos pos, int pPackedLight, int pPackedOverlay)
    {
        pose.pushPose();
        {
            pose.translate(0.9375f, 0, 0);
            renderDynamicModel(CORNER, pose, mainBuffer, Direction.NORTH, level, pos, pPackedLight, pPackedOverlay);
        }
        pose.popPose();
    }
    private void renderCornerBR(PoseStack pose, MultiBufferSource mainBuffer, Level level, BlockPos pos, int pPackedLight, int pPackedOverlay)
    {
        pose.pushPose();
        {
            pose.translate(0.9375f, -0.9375f, 0);
            renderDynamicModel(CORNER, pose, mainBuffer, Direction.NORTH, level, pos, pPackedLight, pPackedOverlay);
        }
        pose.popPose();
    }

    @Override
    public void render(SchematicBoardBlockEntity tile, float pPartialTick, PoseStack pose, MultiBufferSource mainBuffer, int pPackedLight, int pPackedOverlay) {
        NonNullList<ItemStack> inventory = tile.getInventory();
        Level level = tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        BorderState borderState = tile.getBorderState();
        List<Float> randomState = tile.getRandomState();


        rotateForFacing(pose, tile.getFacing());
        pose.pushPose();
        {
            if(borderState.hasLeftEdge()) {
                pose.pushPose();
                {
                    renderDynamicModel(FRAME_EDGE, pose, mainBuffer, Direction.NORTH, level, pos, pPackedLight, pPackedOverlay);
                }
                pose.popPose();
            }

            if(borderState.hasRightEdge()) {
                pose.pushPose();
                {
                    pose.translate(0.9375f, 0, 0);
                    renderDynamicModel(FRAME_EDGE, pose, mainBuffer, Direction.NORTH, level, pos, pPackedLight, pPackedOverlay);
                }
                pose.popPose();
            }

            if(borderState.hasBottomLeftCorner()) {
                renderCornerBL(pose,mainBuffer,level,pos,pPackedLight,pPackedOverlay);
            }

            if(borderState.hasTopLeftCorner()) {
                renderCornerTL(pose,mainBuffer,level,pos,pPackedLight,pPackedOverlay);
            }

            if(borderState.hasBottomRightCorner()) {
                renderCornerBR(pose,mainBuffer,level,pos,pPackedLight,pPackedOverlay);
            }

            if(borderState.hasTopRightCorner()) {
                renderCornerTR(pose,mainBuffer,level,pos,pPackedLight,pPackedOverlay);
            }

            if(borderState.hasBottomEdge()) {
                pose.pushPose();
                {
                    pose.translate(0, 0.0625, 0);
                    pose.mulPose(new Quaternionf().rotateZ(Mth.DEG_TO_RAD * (180 - Direction.UP.toYRot())));
                    renderDynamicModel(FRAME_EDGE, pose, mainBuffer, Direction.NORTH, level, pos, pPackedLight, pPackedOverlay);
                }
                pose.popPose();
            }
            if(borderState.hasTopEdge())
            {
                pose.pushPose();
                {
                    pose.mulPose(new Quaternionf().rotateZ(Mth.DEG_TO_RAD * (180 - Direction.UP.toYRot())));
                    pose.translate(-1f, 0, 0);
                    renderDynamicModel(FRAME_EDGE, pose, mainBuffer, Direction.NORTH, level, pos, pPackedLight, pPackedOverlay);
                }
                pose.popPose();
            }
        }
        pose.popPose();
        assert inventory != null;
        if(!inventory.isEmpty())
        {
            pose.translate(0.09375,-0.125,0.15);
            float xPos = 0;
            float yPos = 0;
            int invSize = inventory.size();
            for (int i = 0; i < invSize; i++) {
                ItemStack item = inventory.get(i);
                if(item.isEmpty()) continue;
                float r = -1 + (randomState.get(i) * 2);
                float randomOffsetX = -1 + (randomState.get(i) * 2);
                float randomOffsetY = -1 + (randomState.get(4+i) * 2);

                if(borderState.hasRightEdge()) randomOffsetX -= 0.5f;
                if(borderState.hasLeftEdge()) randomOffsetX += 0.5f;

                if(borderState.hasTopEdge()) randomOffsetY -= 0.5f;
                if(borderState.hasBottomEdge()) randomOffsetY += 0.5f;

                xPos = i % 2;
                yPos = i < 2 ? 1 : 0;

                if(!(borderState.hasRightEdge() || borderState.hasLeftEdge()))
                {
                    if(xPos == 0) randomOffsetX -= 0.6f;
                    if(xPos == 1) randomOffsetX += 0.6f;
                }

                if(!(borderState.hasTopEdge() || borderState.hasBottomEdge()))
                {
                    if(yPos == 0) randomOffsetY -= 0.6f;
                    if(yPos == 1) randomOffsetY += 0.6f;
                }

                pose.pushPose();
                pose.translate(xPos*0.375, yPos*0.5f, -i * 0.0025f);
                pose.pushPose();
                pose.translate(0.06f * randomOffsetX, 0.04f * randomOffsetY, 0);
                pose.mulPose(new Quaternionf().rotateX(-(2 * r) * Mth.DEG_TO_RAD));
                pose.mulPose(new Quaternionf().rotateZ(-(6 * r) * Mth.DEG_TO_RAD));
                pose.pushPose();
                float s = 1 + (r*0.05f);
                pose.scale(s,s,s);
                renderDynamicModel(SCHEMATIC, pose, mainBuffer, Direction.NORTH, level, pos, pPackedLight, pPackedOverlay);
                pose.popPose();
                pose.popPose();
                pose.popPose();
            }
        }
    }

    private void renderDynamicModel(ESDynamicModel model, PoseStack matrix, MultiBufferSource buffer, Direction facing, Level level, BlockPos pos, int light, int overlay)
    {
        matrix.pushPose();
        List<BakedQuad> quads = model.getNullQuads(ModelData.EMPTY);
        RenderUtils.renderModelTESRFancy(quads, buffer.getBuffer(RenderType.cutoutMipped()), matrix, level, pos, false, 0xf0f0f0, light);
        matrix.popPose();
    }
}
