package tech.muddykat.engineered_schematics.item;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class ESSchematicSettings {
    public static final String KEY_SELF = "settings";
    public static final String KEY_MULTIBLOCK = "multiblock";
    public static final String KEY_MIRROR = "mirror";
    public static final String KEY_PLACED = "placed";
    public static final String KEY_ROTATION = "rotation";
    public static final String KEY_POSITION = "pos";

    private Rotation rotation;
    private BlockPos pos = null;
    private MultiblockHandler.IMultiblock multiblock = null;
    private boolean mirror;
    private boolean isPlaced;

    public ESSchematicSettings(){
        this(new CompoundTag());
    }

    public ESSchematicSettings(@Nullable final ItemStack stack){
        this(((Supplier<CompoundTag>) () -> {
            CompoundTag nbt = null;
            if(stack != null && (nbt = stack.getTagElement(KEY_SELF)) == null){
                nbt = new CompoundTag();
            }
            return nbt;
        }).get());
    }

    public ESSchematicSettings(CompoundTag settingsNbt){
        if(settingsNbt == null || settingsNbt.isEmpty()){
            this.rotation = Rotation.NONE;
            this.mirror = false;
            this.isPlaced = false;
        }else{
            this.rotation = Rotation.values()[settingsNbt.contains(KEY_ROTATION) ? settingsNbt.getInt(KEY_ROTATION) : 0];
            this.mirror = settingsNbt.getBoolean(KEY_MIRROR);
            this.isPlaced = settingsNbt.getBoolean(KEY_PLACED);

            if(settingsNbt.contains(KEY_MULTIBLOCK, Tag.TAG_STRING)){
                String str = settingsNbt.getString("multiblock");
                this.multiblock = MultiblockHandler.getByUniqueName(new ResourceLocation(str));
            }

            if(settingsNbt.contains(KEY_POSITION, Tag.TAG_COMPOUND)){
                CompoundTag pos = settingsNbt.getCompound("pos");
                int x = pos.getInt("x");
                int y = pos.getInt("y");
                int z = pos.getInt("z");
                this.pos = new BlockPos(x, y, z);
            }
        }
    }

    /** Rotate by 90° Clockwise */
    public void rotateCW(){
        this.rotation = this.rotation.getRotated(Rotation.CLOCKWISE_90);
    }

    /** Rotate by 90° Counter-Clockwise */
    public void rotateCCW(){
        this.rotation = this.rotation.getRotated(Rotation.COUNTERCLOCKWISE_90);
    }

    public void flip(){
        this.mirror = !this.mirror;
    }

    public void setRotation(Rotation rotation){
        this.rotation = rotation;
    }

    public void setMultiblock(@Nullable MultiblockHandler.IMultiblock multiblock){
        this.multiblock = multiblock;
    }

    public void setMirror(boolean mirror){
        this.mirror = mirror;
    }

    public void setPlaced(boolean isPlaced){
        this.isPlaced = isPlaced;
    }

    public void setPos(@Nullable BlockPos pos){
        this.pos = pos;
    }

    public Rotation getRotation(){
        return this.rotation;
    }

    public boolean isMirrored(){
        return this.mirror;
    }

    public boolean isPlaced(){
        return this.isPlaced;
    }

    /**
     * May return null to indicate that the projection has not been placed yet
     */
    @Nullable
    public BlockPos getPos(){
        return this.pos;
    }

    /** May return null to indicate no multiblock has been selected yet */
    @Nullable
    public MultiblockHandler.IMultiblock getMultiblock(){
        return this.multiblock;
    }

    public CompoundTag toNbt(){
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(KEY_ROTATION, this.rotation.ordinal());
        nbt.putBoolean(KEY_MIRROR, this.mirror);
        nbt.putBoolean(KEY_PLACED, this.isPlaced);

        if(this.multiblock != null){
            nbt.putString(KEY_MULTIBLOCK, this.multiblock.getUniqueName().toString());
        }

        if(this.pos != null){
            CompoundTag pos = new CompoundTag();
            pos.putInt("x", this.pos.getX());
            pos.putInt("y", this.pos.getY());
            pos.putInt("z", this.pos.getZ());
            nbt.put(KEY_POSITION, pos);
        }

        return nbt;
    }

    public ItemStack applyTo(ItemStack stack){
        stack.getOrCreateTagElement("settings");
        stack.getTag().put("settings", this.toNbt());
        return stack;
    }

    @Override
    public String toString(){
        return "\"Settings\":[" + toNbt().toString() + "]";
    }

    public enum Mode{
        MULTIBLOCK_SELECTION, PROJECTION;

        final String translation;
        Mode(){
            this.translation = "desc.engineered_schematics.info.schematic.mode_" + ordinal();
        }

        public Component getTranslated(){
            return Component.translatable(this.translation);
        }
    }
}