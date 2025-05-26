package tech.muddykat.engineered_schematics.item;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.utils.TemplateWorldCreator;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class SchematicProjection
{
    final MultiblockHandler.IMultiblock multiblock;
    final Level realWorld;
    final Level templateWorld;
    final StructurePlaceSettings settings = new StructurePlaceSettings();
    final Int2ObjectMap<List<StructureTemplate.StructureBlockInfo>> layers = new Int2ObjectArrayMap<>();
    final BlockPos.MutableBlockPos offset = new BlockPos.MutableBlockPos();
    final int blockcount;
    boolean isDirty = true;
    public SchematicProjection(@Nonnull Level world, @Nonnull MultiblockHandler.IMultiblock multiblock){
        Objects.requireNonNull(world, "World cannot be null!");
        Objects.requireNonNull(multiblock, "Multiblock cannot be null!");
        this.multiblock = multiblock;
        this.realWorld = world;

        List<StructureTemplate.StructureBlockInfo> blocks = multiblock.getStructure(world);
        this.templateWorld = TemplateWorldCreator.CREATOR.getValue().makeWorld(blocks, pos -> true, world.registryAccess());
        this.blockcount = blocks.size();
        for (StructureTemplate.StructureBlockInfo info : blocks) {
            List<StructureTemplate.StructureBlockInfo> list = this.layers.get(info.pos().getY());
            if (list == null) {
                list = new ArrayList<>();
                this.layers.put(info.pos().getY(), list);
            }

            list.add(info);
        }
    }

    public SchematicProjection setRotation(Rotation rotation){
        if(this.settings.getRotation() != rotation){
            this.settings.setRotation(rotation);
            this.isDirty = true;
        }

        return this;
    }

    /**
     * Sets the mirrored state.
     *
     * <pre>
     * true = {@link Mirror#FRONT_BACK}
     *
     * false = {@link Mirror#NONE}
     * </pre>
     */
    public SchematicProjection setFlip(boolean mirror){
        Mirror m = mirror ? Mirror.FRONT_BACK : Mirror.NONE;
        if(this.settings.getMirror() != m){
            this.settings.setMirror(m);
            this.isDirty = true;
        }

        return this;
    }

    public void reset(){
        this.settings.setRotation(Rotation.NONE);
        this.settings.setMirror(Mirror.NONE);
        this.offset.set(0, 0, 0);
    }

    /** Total amount of blocks present in the multiblock */
    public int getBlockCount(){
        return this.blockcount;
    }

    /** Amount of layers in this projection */
    public int getLayerCount(){
        return this.layers.size();
    }

    public int getLayerSize(int layer){
        if(layer < 0 || layer >= this.layers.size()){
            return 0;
        }

        return this.layers.get(layer).size();
    }

    public Level getTemplateWorld(){
        return this.templateWorld;
    }

    public MultiblockHandler.IMultiblock getMultiblock(){
        return this.multiblock;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj)
            return true;
        if(obj instanceof SchematicProjection other){
            return this.multiblock.getUniqueName().equals(other.multiblock.getUniqueName()) &&
                    this.settings.getMirror() == other.settings.getMirror() &&
                    this.settings.getRotation() == other.settings.getRotation();
        }

        return false;
    }

    /**
     * Single-Layer based projection processing
     *
     * @param layer     The layer to work on
     * @param predicate What to do per block
     * @return true if it was interrupted
     */
    public boolean process(int layer, Predicate<Info> predicate){
        updateData();

        List<StructureTemplate.StructureBlockInfo> blocks = this.layers.get(layer);
        if(blocks == null) return false;
        for(StructureTemplate.StructureBlockInfo info:blocks){
            if(predicate.test(new Info(this, info))){
                return true;
            }
        }

        return false;
    }

    /**
     * Multi-Layer based projection processing. (Do all at once)
     *
     * @param predicate What to do per block
     * @return true if it was stopped pre-maturely, false if it went through everything
     */
    public boolean processAll(BiPredicate<Integer, Info> predicate){
        updateData();

        for(int layer = 0;layer < getLayerCount();layer++){
            List<StructureTemplate.StructureBlockInfo> blocks = this.layers.get(layer);
            for(StructureTemplate.StructureBlockInfo info:blocks){
                if(predicate.test(layer, new Info(this, info))){
                    return true;
                }
            }
        }
        return false;
    }

    private void updateData(){
        if(!this.isDirty)
            return;
        this.isDirty = false;

        boolean mirrored = this.settings.getMirror() == Mirror.FRONT_BACK;
        Rotation rotation = this.settings.getRotation();
        Vec3i size = this.multiblock.getSize(this.realWorld);

        // Align corners first
        if(!mirrored){
            switch(rotation){
                case CLOCKWISE_90 ->		this.offset.set(1 - size.getZ(), 0, 0);
                case CLOCKWISE_180 ->		this.offset.set(1 - size.getX(), 0, 1 - size.getZ());
                case COUNTERCLOCKWISE_90 ->	this.offset.set(0, 0, 1 - size.getX());
                default ->					this.offset.set(0, 0, 0);
            }
        }else{
            switch(rotation){
                case NONE ->			this.offset.set(1 - size.getX(), 0, 0);
                case CLOCKWISE_90 ->	this.offset.set(1 - size.getZ(), 0, 1 - size.getX());
                case CLOCKWISE_180 ->	this.offset.set(0, 0, 1 - size.getZ());
                default ->				this.offset.set(0, 0, 0);
            }
        }

        // Center the whole thing
        int x = ((rotation.ordinal() % 2 == 0) ? size.getX() : size.getZ()) / 2;
        int z = ((rotation.ordinal() % 2 == 0) ? size.getZ() : size.getX()) / 2;
        this.offset.setWithOffset(this.offset, x, 0, z);
    }

    public BlockPos getRealPos(BlockPos offset)
    {
        List<StructureTemplate.StructureBlockInfo> blocks = this.layers.get(0);
        for(StructureTemplate.StructureBlockInfo info:blocks){
            Info inf = new Info(this, info);
            return inf.tPos.offset(offset);
        }
        return offset;
    }

    // STATIC CLASSES

    public static final class Info{

        /** Currently applied template transformation */
        public final StructurePlaceSettings settings;

        /** The multiblock being processed */
        public final MultiblockHandler.IMultiblock multiblock;

        /** Transformed Template Position */
        public final BlockPos tPos;

        public final Level templateWorld;

        public final StructureTemplate.StructureBlockInfo tBlockInfo;

        public Info(SchematicProjection projection, StructureTemplate.StructureBlockInfo templateBlockInfo){
            this.multiblock = projection.multiblock;
            this.templateWorld = projection.templateWorld;
            this.settings = projection.settings;
            this.tBlockInfo = templateBlockInfo;
            this.tPos = StructureTemplate.calculateRelativePosition(this.settings, templateBlockInfo.pos()).subtract(projection.offset);
        }

        /** Convenience method for getting the state with mirror and rotation already applied */
        public BlockState getModifiedState(Level realWorld, BlockPos realPos){
            return this.templateWorld.getBlockState(this.tBlockInfo.pos())
                    .mirror(this.settings.getMirror())
                    .rotate(realWorld, realPos, this.settings.getRotation());
        }

        public BlockState getRawState(){
            return this.templateWorld.getBlockState(this.tBlockInfo.pos());
        }
    }
}
