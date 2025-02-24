package tech.muddykat.engineered_schematics.helper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.joml.Vector3f;
import tech.muddykat.engineered_schematics.client.ESShaders;
import tech.muddykat.engineered_schematics.item.ESSchematicSettings;
import tech.muddykat.engineered_schematics.item.SchematicProjection;
import tech.muddykat.engineered_schematics.registry.ESRenderTypes;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;

public class SchematicRenderer
{
    private static final int COLOR_ERROR = 0xFF0000;
    private static final int COLOR_WARNING = 0xFFFF00;
    private static final int COLOR_SUCCESS = 0x00BF00;
    private static final int COLOR_HIGHLIGHT = 0x44FF44;
    private static final float[] COLOR_HELD = {0.2f, 1.0f, 0.5f};
    private static final float[] COLOR_NORMAL = {0.2f, 0.5f, 1.0f};
    static final BlockPos.MutableBlockPos FULL_MAX = new BlockPos.MutableBlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    public static void renderSchematic(PoseStack matrix, ESSchematicSettings settings,
                                       Player player, Level world) {
        if (settings.getMultiblock() == null) return;

        // Initialize position tracking
        final BlockPos.MutableBlockPos hit = initializePosition(settings);
        if (hit.equals(FULL_MAX)) return;

        // Setup projection
        SchematicProjection projection = setupProjection(world, settings);
        Vec3i mbSize = settings.getMultiblock().getSize(world);

        // Pre-allocate collections with estimated sizes
        Map<BlockPos, Boolean> badStates = new HashMap<>(mbSize.getX() * mbSize.getY() * mbSize.getZ());
        List<Pair<RenderLayer, SchematicProjection.Info>> toRender = new ArrayList<>(projection.getBlockCount());

        // Process blocks
        RenderingState renderState = processBlocks(projection, world, hit, badStates, toRender);

        // Render results
        renderResults(matrix, renderState, settings, world, player, mbSize,
                badStates, toRender);
    }

    private static BlockPos.MutableBlockPos initializePosition(ESSchematicSettings settings) {
        final BlockPos.MutableBlockPos hit = new BlockPos.MutableBlockPos(FULL_MAX.getX(), FULL_MAX.getY(), FULL_MAX.getZ());
        if (settings.getPos() != null) {
            hit.set(settings.getPos());
        }
        return hit;
    }

    private static SchematicProjection setupProjection(Level world, ESSchematicSettings settings) {
        SchematicProjection projection = new SchematicProjection(world, settings.getMultiblock());
        projection.setRotation(settings.getRotation());
        projection.setFlip(settings.isMirrored());
        return projection;
    }

    private static RenderingState processBlocks(SchematicProjection projection, Level world,
                                                BlockPos hit, Map<BlockPos, Boolean> badStates, List<Pair<RenderLayer, SchematicProjection.Info>> toRender) {
        MutableInt currentLayer = new MutableInt();
        MutableInt badBlocks = new MutableInt();
        MutableInt goodBlocks = new MutableInt();
        AtomicInteger imperfectionLayer = new AtomicInteger(-1);

        projection.processAll((layer, info) -> processBlock(layer, info, world, hit,
                badStates, toRender, currentLayer, badBlocks, goodBlocks, imperfectionLayer));

        return new RenderingState(
                goodBlocks.getValue() == projection.getBlockCount(),
                imperfectionLayer.get() != -1,
                badStates.containsValue(false),
                currentLayer.getValue()
        );
    }

    private static boolean processBlock(int layer, SchematicProjection.Info info, Level world, BlockPos hit,
                                        Map<BlockPos, Boolean> badStates, List<Pair<RenderLayer, SchematicProjection.Info>> toRender,
                                        MutableInt currentLayer, MutableInt badBlocks, MutableInt goodBlocks,
                                        AtomicInteger imperfectionLayer) {

        // Update current layer if no bad blocks found
        if (badBlocks.getValue() == 0 && layer > currentLayer.getValue()) {
            currentLayer.setValue(layer);
        } else if (layer != currentLayer.getValue()) {
            return true; // Break the internal loop
        }

        // Process block at current layer
        if (hit != FULL_MAX && layer == currentLayer.getValue()) {
            BlockPos realPos = info.tPos.offset(hit);
            BlockState currentState = world.getBlockState(realPos);
            BlockState targetState = info.getModifiedState(world, realPos);

            // Check if block is in correct state
            if (targetState == currentState) {
                toRender.add(Pair.of(RenderLayer.PERFECT, info));
                goodBlocks.increment();
                return false;
            }

            // Check if block needs replacing
            if (!currentState.isAir()) {
                toRender.add(Pair.of(RenderLayer.BAD, info));
                // Check if block is of correct type but wrong state
                boolean isSameBlockType = targetState.getBlock().defaultBlockState()
                        .equals(currentState.getBlock().defaultBlockState());
                badStates.put(info.tPos, isSameBlockType);
                imperfectionLayer.set(layer);
                return false;
            }

            badBlocks.increment();
        }

        toRender.add(Pair.of(RenderLayer.ALL, info));
        return false;
    }

    private static void renderResults(PoseStack matrix, RenderingState state,
                                      ESSchematicSettings settings, Level world, Player player, Vec3i mbSize,
                                      Map<BlockPos, Boolean> badStates, List<Pair<RenderLayer, SchematicProjection.Info>> toRender) {

        // Sort render list by layer type
        toRender.sort(Comparator.comparingInt(a -> a.getFirst().ordinal()));

        // Setup rendering
        matrix.translate(settings.getPos().getX(), settings.getPos().getY(), settings.getPos().getZ());
        MultiBufferSource.BufferSource mainBuffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        // Render grid for imperfect structures
        if (!state.perfect()) {
            matrix.pushPose();
            renderGridForRotation(matrix, mainBuffer, settings, mbSize, state);
            matrix.popPose();
        }

        // Track perfect structure bounds
        BlockPos.MutableBlockPos min = new BlockPos.MutableBlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        BlockPos.MutableBlockPos max = new BlockPos.MutableBlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        ItemStack heldStack = player.getMainHandItem();

        // Render each block
        for (Pair<RenderLayer, SchematicProjection.Info> pair : toRender) {
            SchematicProjection.Info info = pair.getSecond();
            boolean isHeld = heldStack.getItem() == info.getRawState().getBlock().asItem();

            switch (pair.getFirst()) {
                case ALL -> renderAllLayer(matrix, world, info, isHeld, state.hasWrongBlock(), mainBuffer);

                case BAD -> renderBadLayer(matrix, mainBuffer, info, badStates);

                case PERFECT -> updatePerfectBounds(min, max, info);
            }
        }

        // Render perfect structure outline
        if (state.perfect()) {
            matrix.pushPose();
            renderOutlineBox(mainBuffer, matrix, min, max, COLOR_SUCCESS);
            matrix.popPose();
        }

        mainBuffer.endBatch();
    }
    private static void renderGridForRotation(PoseStack matrix, MultiBufferSource.BufferSource buffer,
                                              ESSchematicSettings settings, Vec3i mbSize, RenderingState state) {
        boolean isHorizontalRotation = settings.getRotation().equals(Rotation.CLOCKWISE_180) ||
                settings.getRotation().equals(Rotation.NONE);

        int width = isHorizontalRotation ? mbSize.getX() : mbSize.getZ();
        int depth = isHorizontalRotation ? mbSize.getZ() : mbSize.getX();

        matrix.translate(
                -(Math.floorDiv(width, 2)),
                -mbSize.getY(),
                -(Math.floorDiv(depth, 2))
        );
        matrix.translate(0, state.currentLayer(), 0);

        int highlightColor = state.hasImperfection() ?
                (state.hasWrongBlock() ? COLOR_ERROR : COLOR_WARNING) : 0xffffff;

        renderGrid(buffer, matrix, Vec3.ZERO,
                new Vec3(width, mbSize.getY(), depth), 16, 0.25f, highlightColor);
    }

    private static void renderAllLayer(PoseStack matrix, Level world, SchematicProjection.Info info, boolean isHeld, boolean hasWrongBlock,
                                       MultiBufferSource.BufferSource buffer) {
        if (hasWrongBlock) return;

        matrix.pushPose();
        renderPhantom(matrix, world, info,
                isHeld ? COLOR_HELD : COLOR_NORMAL, 0);

        if (isHeld) {
            renderCenteredOutlineBox(buffer, matrix, COLOR_HIGHLIGHT);
        }
        matrix.popPose();
    }

    private static void renderBadLayer(PoseStack matrix, MultiBufferSource.BufferSource buffer,
                                       SchematicProjection.Info info, Map<BlockPos, Boolean> badStates) {
        matrix.pushPose();
        matrix.translate(info.tPos.getX(), info.tPos.getY(), info.tPos.getZ());
        renderCenteredOutlineBox(buffer, matrix,
                badStates.get(info.tPos) ? COLOR_WARNING : COLOR_ERROR);
        matrix.popPose();
    }

    private static void updatePerfectBounds(BlockPos.MutableBlockPos min, BlockPos.MutableBlockPos max, SchematicProjection.Info info) {
        min.set(
                Math.min(info.tPos.getX(), min.getX()),
                Math.min(info.tPos.getY(), min.getY()),
                Math.min(info.tPos.getZ(), min.getZ())
        );
        max.set(
                Math.max(info.tPos.getX(), max.getX()),
                Math.max(info.tPos.getY(), max.getY()),
                Math.max(info.tPos.getZ(), max.getZ())
        );
    }

    private static final Tesselator PHANTOM_TESSELATOR = new Tesselator();
    public static void renderPhantom(PoseStack matrix, Level realWorld, SchematicProjection.Info rInfo, float[] color, float partialTicks){
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        ModelBlockRenderer blockRenderer = dispatcher.getModelRenderer();
        BlockColors blockColors = Minecraft.getInstance().getBlockColors();

        // Centers the preview block
        matrix.translate(rInfo.tPos.getX(), rInfo.tPos.getY(), rInfo.tPos.getZ());

        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(PHANTOM_TESSELATOR.getBuilder());

        BlockState state = rInfo.getModifiedState(realWorld, rInfo.tPos);
        state.updateNeighbourShapes(realWorld, rInfo.tPos, 3);

        ModelData modelData = ModelData.EMPTY;
        BlockEntity te = rInfo.templateWorld.getBlockEntity(rInfo.tBlockInfo.pos());
        if(te != null){
            te.setBlockState(state);
            modelData = te.getModelData();
        }

        RenderShape blockrendertype = state.getRenderShape();
        switch(blockrendertype){
            case MODEL -> {
                BakedModel ibakedmodel = dispatcher.getBlockModel(state);
                int i = blockColors.getColor(state, null, null, 0);
                float red = (i >> 16 & 0xFF) / 255F;
                float green = (i >> 8 & 0xFF) / 255F;
                float blue = (i & 0xFF) / 255F;

                modelData = ibakedmodel.getModelData(rInfo.templateWorld, rInfo.tBlockInfo.pos(), state, modelData);
                ESShaders.setSchematicRenderData(partialTicks, color[0],color[1],color[2]);
                VertexConsumer vc = buffer.getBuffer(ESRenderTypes.SCHEMATIC);
                matrix.scale(0.5f, 0.5f,0.5f);
                matrix.translate(0.5f,0.5f,0.5f);
                blockRenderer.renderModel(matrix.last(), vc, state, ibakedmodel, red, green, blue, 0xF000F0, OverlayTexture.NO_OVERLAY, modelData, null);
                matrix.translate(-0.5f,-0.5f,-0.5f);
                matrix.scale(2f, 2f,2f);
            }
            case ENTITYBLOCK_ANIMATED -> {
                ItemStack stack = new ItemStack(state.getBlock());
                matrix.scale(0.5f, 0.5f,0.5f);
                matrix.translate(0.5f,0.5f,0.5f);
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, 0xF000F0, OverlayTexture.NO_OVERLAY, matrix, buffer, null, 0);
                matrix.translate(-0.5f,-0.5f,-0.5f);
                matrix.scale(2f, 2f,2f);
            }
            default -> {}
        }

        buffer.endBatch();
    }

    public static void renderCenteredOutlineBox(MultiBufferSource buffer, PoseStack matrix, int rgb){
        renderBox(buffer, matrix, Vec3.ZERO, new Vec3(1, 1, 1), rgb);
    }

    public static void renderOutlineBox(MultiBufferSource buffer, PoseStack matrix, Vec3i min, Vec3i max, int rgb){
        renderBox(buffer, matrix, Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max).add(1, 1, 1), rgb);
    }

    public static void renderGrid(
            MultiBufferSource buffer,
            PoseStack matrix,
            Vec3 origin,
            Vec3 normal,
            float gridSize,
            float stepSize,
            int rgb
    ) {
        VertexConsumer builder = buffer.getBuffer(RenderType.LINES);
        float alpha = 0.3f;
        int rgba = rgb | (((int) (alpha * 255)) << 24);

        for(float a = 0; a <= normal.z; a+=stepSize)
        {
            matrix.translate(0,0,a);
            line(builder, matrix, origin, origin.add(normal), 0b010, 0b110, rgba);
            matrix.translate(0,0,-a);
        }
        for(float a = 0; a <= normal.x; a+=stepSize)
        {
            matrix.translate(a, 0, 0);
            line(builder, matrix, origin, origin.add(normal), 0b010, 0b011, rgba);
            matrix.translate(-a, 0, 0);
        }
    }

    public static void renderSchematicGrid(PoseStack matrix, ESSchematicSettings settings, Level world)
    {
        if(settings.getMultiblock() == null) return;
        final BlockPos.MutableBlockPos hit = new BlockPos.MutableBlockPos(FULL_MAX.getX(), FULL_MAX.getY(), FULL_MAX.getZ());
        final MutableBoolean isPlaced = new MutableBoolean(false);
        if(settings.getPos() != null)
        {
            hit.set(settings.getPos());
            isPlaced.setTrue();
        }

        if(!hit.equals(FULL_MAX))
        {
            SchematicProjection projection = new SchematicProjection(world, settings.getMultiblock());
            projection.setRotation(settings.getRotation());
            projection.setFlip(settings.isMirrored());
            Vec3i mb_size = settings.getMultiblock().getSize(world);
            matrix.translate(hit.getX(), hit.getY(), hit.getZ());

            MultiBufferSource.BufferSource mainBuffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            matrix.pushPose();
            {
                if(settings.getRotation().equals(Rotation.CLOCKWISE_180) || settings.getRotation().equals(Rotation.NONE))
                {
                    matrix.translate(-(Math.floorDiv(mb_size.getX(),2)), -mb_size.getY(), -(Math.floorDiv(mb_size.getZ(),2)));
                    renderGrid(mainBuffer, matrix, Vec3.ZERO, new Vec3(mb_size.getX(), mb_size.getY(), mb_size.getZ()), 16, 0.25f, 0xffffff);
                }
                else
                {
                    matrix.translate(-(Math.floorDiv(mb_size.getZ(),2)), -mb_size.getY(), -(Math.floorDiv(mb_size.getX(),2)));
                    renderGrid(mainBuffer, matrix, Vec3.ZERO, new Vec3(mb_size.getZ(), mb_size.getY(), mb_size.getX()), 16, 0.25f, 0xffffff);
                }
            }
            matrix.popPose();

            mainBuffer.endBatch();
        }
    }

    private static void renderBox(MultiBufferSource buffer, PoseStack matrix, Vec3 min, Vec3 max, int rgb){
        VertexConsumer builder = buffer.getBuffer(RenderType.LINES);

        float alpha = 0.3f;

        int rgba = rgb | (((int) (alpha * 255)) << 24);

        line(builder, matrix, min, max, 0b010, 0b110, rgba);
        line(builder, matrix, min, max, 0b110, 0b111, rgba);
        line(builder, matrix, min, max, 0b111, 0b011, rgba);
        line(builder, matrix, min, max, 0b011, 0b010, rgba);

        line(builder, matrix, min, max, 0b010, 0b000, rgba);
        line(builder, matrix, min, max, 0b110, 0b100, rgba);
        line(builder, matrix, min, max, 0b011, 0b001, rgba);
        line(builder, matrix, min, max, 0b111, 0b101, rgba);

        line(builder, matrix, min, max, 0b000, 0b100, rgba);
        line(builder, matrix, min, max, 0b100, 0b101, rgba);
        line(builder, matrix, min, max, 0b101, 0b001, rgba);
        line(builder, matrix, min, max, 0b001, 0b000, rgba);
    }

    private static void line(VertexConsumer out, PoseStack mat, Vec3 min, Vec3 max, int startBits, int endBits, int rgba){
        Vector3f start = combine(min, max, startBits);
        Vector3f end = combine(min, max, endBits);
        Vector3f delta = new Vector3f(end);
        delta.sub(start);
        out.vertex(mat.last().pose(), start.x(), start.y(), start.z())
                .color(rgba)
                .normal(mat.last().normal(), delta.x(), delta.y(), delta.z())
                .endVertex();
        out.vertex(mat.last().pose(), end.x(), end.y(), end.z())
                .color(rgba)
                .normal(mat.last().normal(), delta.x(), delta.y(), delta.z())
                .endVertex();
    }

    private static Vector3f combine(Vec3 start, Vec3 end, int mixBits){
        final float eps = 0.01f;
        return new Vector3f(
                (float) ((mixBits & 4) != 0 ? end.x + eps : start.x - eps),
                (float) ((mixBits & 2) != 0 ? end.y + eps : start.y - eps),
                (float) ((mixBits & 1) != 0 ? end.z + eps : start.z - eps)
        );
    }

    private record RenderingState(
            boolean perfect,
            boolean hasImperfection,
            boolean hasWrongBlock,
            int currentLayer
    ) {}

    public enum RenderLayer{
        ALL, BAD, PERFECT
    }
}
