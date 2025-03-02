package tech.muddykat.engineered_schematics.helper;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockBE;
import blusunrize.immersiveengineering.common.register.IEItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tech.muddykat.engineered_schematics.client.ESShaders;
import tech.muddykat.engineered_schematics.item.ESSchematicSettings;
import tech.muddykat.engineered_schematics.item.SchematicProjection;
import tech.muddykat.engineered_schematics.registry.ESRenderTypes;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
        assert(settings.getPos() != null);
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
        for(Pair<RenderLayer, SchematicProjection.Info> pair : toRender) {
            SchematicProjection.Info info = pair.getSecond();
            BlockEntity be = world.getBlockEntity(info.tPos.offset(settings.getPos()));
            if (be instanceof IMultiblockBE<?>)
            {
                renderSchematicGrid(matrix, settings, 0x009900, world);
                return;
            }
        }
        // Render results
        MultiBufferSource.BufferSource mainBuffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        renderResults(matrix, renderState, settings, mainBuffer, world, player, mbSize,
                badStates, toRender);
        mainBuffer.endBatch();
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
                                      ESSchematicSettings settings, MultiBufferSource.BufferSource mainBuffer, Level world, Player player, Vec3i mbSize,
                                      Map<BlockPos, Boolean> badStates, List<Pair<RenderLayer, SchematicProjection.Info>> toRender) {

        // Sort render list by layer type
        matrix.pushPose();
        toRender.sort(Comparator.comparingInt(a -> a.getFirst().ordinal()));
        // Setup rendering
        matrix.translate(settings.getPos().getX(), settings.getPos().getY(), settings.getPos().getZ());

        // Track perfect structure bounds
        BlockPos.MutableBlockPos min = new BlockPos.MutableBlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        BlockPos.MutableBlockPos max = new BlockPos.MutableBlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        ItemStack heldStack = player.getMainHandItem();

        // Render each block
        int gridLayer = state.currentLayer();
        for (Pair<RenderLayer, SchematicProjection.Info> pair : toRender) {
            SchematicProjection.Info info = pair.getSecond();
            boolean isHeld = heldStack.getItem() == info.getRawState().getBlock().asItem();
            switch (pair.getFirst()) {
                case ALL -> renderAllLayer(matrix, world, info, isHeld, state.hasWrongBlock(), mainBuffer);
                case BAD -> renderBadLayer(matrix, mainBuffer, info, badStates);
                case PERFECT -> updatePerfectBounds(min, max, info);
            }
        }

        for(BlockPos badBlocks : badStates.keySet())
        {
            gridLayer = Math.min(gridLayer, badBlocks.getY());
        }

        if (!state.perfect()) {
            matrix.pushPose();
            renderGridForRotation(matrix, mainBuffer, settings, mbSize, state, gridLayer);
            matrix.popPose();
        }
        matrix.popPose();
        // Render perfect structure outline
        if (state.perfect()) {
            assert(settings.getMultiblock() != null);
            BlockPos formPos = settings.getPos().offset(-Mth.floorDiv(mbSize.getX(),2), 0, -Mth.floorDiv(mbSize.getZ(),2)).offset(settings.getMultiblock().getTriggerOffset());
            matrix.pushPose();

            matrix.translate(formPos.getX(), formPos.getY(), formPos.getZ() + .5f);
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            ItemStack hammerStack = new ItemStack(IEItems.Tools.HAMMER);
            BakedModel itemModel = itemRenderer.getModel(hammerStack, world, null, 0);
            int hammer = 0xaa0000;
            float red = (hammer >> 16 & 0xFF) / 255F;
            float green = (hammer >> 8 & 0xFF) / 255F;
            float blue = (hammer & 0xFF) / 255F;

            ESShaders.setSchematicRenderData(world.getGameTime(), red,green,blue);
            MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
            VertexConsumer vertexConsumer = buffer.getBuffer(ESRenderTypes.SCHEMATIC);

            itemRenderer.renderModelLists(itemModel, hammerStack, 15728880, OverlayTexture.NO_OVERLAY, matrix, vertexConsumer);

            matrix.popPose();
            BlockPos pos = settings.getPos();
            BlockPos offset = new BlockPos(-Mth.floorDiv(mbSize.getX(),2), 0, -Mth.floorDiv(mbSize.getZ(),2));

            matrix.translate(pos.getX(),pos.getY(),pos.getZ());
            renderOutlineBox(mainBuffer, matrix, min, max, COLOR_SUCCESS);
            matrix.pushPose();

            matrix.translate(offset.getX(),offset.getY(),offset.getZ());
            drawFrontGroundText(matrix, new Vec3((int)mbSize.getX(),0,(int)mbSize.getZ()), mainBuffer, settings.getRotation(), settings.getMultiblock(),0x66ffffff, Component.translatable("item.engineered_schematics.multiblock_schematic.formed"));
            matrix.popPose();
        }
    }
    private static void renderGridForRotation(PoseStack matrix, MultiBufferSource.BufferSource buffer,
                                              ESSchematicSettings settings, Vec3i mbSize, RenderingState state, int y) {
        Rotation rotation = settings.getRotation();
        assert(settings.getMultiblock()!=null);
        // Determine width and depth based on rotation
        boolean isHorizontalRotation = rotation == Rotation.NONE || rotation == Rotation.CLOCKWISE_180;
        int width = isHorizontalRotation ? mbSize.getX() : mbSize.getZ();
        int depth = isHorizontalRotation ? mbSize.getZ() : mbSize.getX();

        // Initial translation to center the grid
        matrix.pushPose();
        matrix.translate(
                -Math.floorDiv(width, 2),
                -mbSize.getY(),
                -Math.floorDiv(depth, 2)
        );
        matrix.translate(0, y, 0);

        // Render the main grid
        int highlightColor = state.hasImperfection() ?
                (state.hasWrongBlock() ? COLOR_ERROR : COLOR_WARNING) : 0xffffff;
        Vec3 origin = Vec3.ZERO;
        Vec3 normal = new Vec3(width, mbSize.getY(), depth);
        Component name = state.hasImperfection() ?
                (state.hasWrongBlock() ? Component.translatable("item.engineered_schematics.multiblock_schematic.error") : settings.getMultiblock().getDisplayName()) : settings.getMultiblock().getDisplayName();
        renderGrid(buffer, matrix, origin, normal, 16, 1f, highlightColor, y, name, settings);
        matrix.popPose();
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
                ESShaders.setSchematicRenderData(0, color[0],color[1],color[2]);
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
            int rgb,
            int currentLayer,
            Component name,
            ESSchematicSettings settings
    ) {
        VertexConsumer builder = buffer.getBuffer(RenderType.LINES);
        Rotation rotation = settings.getRotation();
        MultiblockHandler.IMultiblock multiblock = settings.getMultiblock();
        assert(multiblock != null);
        float alpha = 0.3f;
        int rgba = rgb | (((int) (alpha * 255)) << 24);
        int color = rgba;
        int guideColor = 0x0000ff00;
        Vec3 max = origin.add(normal);


        matrix.pushPose();
        {
            boolean isDebug = Minecraft.getInstance().getDebugOverlay().showDebugScreen();
            for (float a = 0; a <= normal.z; a += stepSize) {
                rgba = color;
                if ((a == 0 || a == normal.z) && isDebug) {
                    if (rotation.equals(Rotation.NONE) && a == 0) rgba = 0x66ff0000;
                    if (rotation.equals(Rotation.CLOCKWISE_180) && a != 0) rgba = 0x66ff0000;
                    if (rotation.equals(Rotation.CLOCKWISE_90) && a == 0) rgba = 0x660000ff;
                    if (rotation.equals(Rotation.COUNTERCLOCKWISE_90) && a != 0) rgba = 0x660000ff;
                }
                matrix.translate(0, 0, a);
                line(builder, matrix, origin, max, 0b010, 0b110, rgba);
                matrix.translate(0, 0, -a);
            }
            for (float a = 0; a <= normal.x; a += stepSize) {
                rgba = color;
                if ((a == 0 || a == normal.x) && isDebug) {
                    if (rotation.equals(Rotation.NONE) && a == 0) rgba = 0x660000ff;
                    if (rotation.equals(Rotation.CLOCKWISE_90) && a != 0) rgba = 0x66ff0000;
                    if (rotation.equals(Rotation.COUNTERCLOCKWISE_90) && a == 0) rgba = 0x66ff0000;
                    if (rotation.equals(Rotation.CLOCKWISE_180) && a != 0) rgba = 0x660000ff;
                }
                matrix.translate(a, 0, 0);
                line(builder, matrix, origin, max, 0b010, 0b011, rgba);
                matrix.translate(-a, 0, 0);
            }

            if (isDebug) {
                matrix.pushPose();
                matrix.translate(0, normal.y, 0);
                guideColor = guideColor | 0x66000000;
                if (rotation.equals(Rotation.NONE)) line(builder, matrix, origin, max, 0b010, 0b000, guideColor);
                if (rotation.equals(Rotation.CLOCKWISE_90))
                    line(builder, matrix, origin, max, 0b110, 0b100, guideColor);
                if (rotation.equals(Rotation.COUNTERCLOCKWISE_90))
                    line(builder, matrix, origin, max, 0b011, 0b001, guideColor);
                if (rotation.equals(Rotation.CLOCKWISE_180))
                    line(builder, matrix, origin, max, 0b111, 0b101, guideColor);
                matrix.popPose();
            }
        }
        matrix.popPose();
        drawFrontGroundText(matrix,max.subtract(0,currentLayer,0), buffer, rotation, multiblock, color,name);
    }

    public static void drawFrontGroundText(PoseStack matrix, Vec3 mbSize, MultiBufferSource buffer, Rotation rotation, MultiblockHandler.IMultiblock multiblock, int color, Component text)
    {

        matrix.translate(0, mbSize.y, 0);
        matrix.pushPose();
        {
            Font font = Minecraft.getInstance().font;
            matrix.translate(0, 0.0125f, 0);
            if (rotation.equals(Rotation.NONE)) matrix.translate(0, 0, mbSize.z() + 0.125f);
            if (rotation.equals(Rotation.CLOCKWISE_90)) matrix.translate(-0.125f, 0, 0);
            if (rotation.equals(Rotation.COUNTERCLOCKWISE_90)) matrix.translate(mbSize.x() + 0.125f, 0, mbSize.z());
            if (rotation.equals(Rotation.CLOCKWISE_180)) matrix.translate(mbSize.x(), 0, -0.125f);
            matrix.mulPose(new Quaternionf().rotateAxis(90 * Mth.DEG_TO_RAD, 1, 0, 0));
            matrix.mulPose(new Quaternionf().rotateAxis((90 * rotation.ordinal()) * Mth.DEG_TO_RAD, 0, 0, 1));
            matrix.scale(0.1f, 0.1f, 0.1f);
            float s = (float) (mbSize.x / text.getString().length());
            matrix.scale(s, s, s);
            font.drawInBatch(text,
                    0, 0, // x and y (already translated)
                    color,
                    false, // drop shadow
                    matrix.last().pose(),
                    buffer,
                    Font.DisplayMode.NORMAL, // NORMAL or SEE_THROUGH for transparency effects
                    15728880, // packed light
                    15728880);
        }
        matrix.popPose();
    }

    public static void renderSchematicGrid(PoseStack matrix, ESSchematicSettings settings, int color, Level world)
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
            Rotation rotation = settings.getRotation();
            projection.setRotation(rotation);
            projection.setFlip(settings.isMirrored());
            Vec3i mb_size = settings.getMultiblock().getSize(world);
            matrix.translate(hit.getX(), hit.getY(), hit.getZ());
            Component name = settings.getMultiblock().getDisplayName();
            MultiBufferSource.BufferSource mainBuffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            matrix.pushPose();
            {
                if(settings.getRotation().equals(Rotation.CLOCKWISE_180) || settings.getRotation().equals(Rotation.NONE))
                {
                    matrix.translate(-(Math.floorDiv(mb_size.getX(),2)), -mb_size.getY(), -(Math.floorDiv(mb_size.getZ(),2)));
                    renderGrid(mainBuffer, matrix, Vec3.ZERO, new Vec3(mb_size.getX(), mb_size.getY(), mb_size.getZ()), 16, 1, color, 0,name, settings);
                }
                else
                {
                    matrix.translate(-(Math.floorDiv(mb_size.getZ(),2)), -mb_size.getY(), -(Math.floorDiv(mb_size.getX(),2)));
                    renderGrid(mainBuffer, matrix, Vec3.ZERO, new Vec3(mb_size.getZ(), mb_size.getY(), mb_size.getX()), 16, 1, color, 0,name, settings);
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
