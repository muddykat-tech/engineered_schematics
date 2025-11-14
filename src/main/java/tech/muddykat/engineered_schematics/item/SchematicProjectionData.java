package tech.muddykat.engineered_schematics.item;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.registry.ESDataComponents;

import java.util.Optional;
import java.util.function.BiConsumer;

public record SchematicProjectionData(
        ItemStack formationTool,
        Rotation rotation,
        boolean mirror,
        boolean isPlaced,
        Optional<BlockPos> pos,
        Optional<ResourceLocation> multiblock
) {
    public static final Codec<SchematicProjectionData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    ItemStack.CODEC.fieldOf("formation_tool").forGetter(SchematicProjectionData::formationTool),
                    Rotation.CODEC.fieldOf("rotation").forGetter(SchematicProjectionData::rotation),
                    Codec.BOOL.fieldOf("mirror").forGetter(SchematicProjectionData::mirror),
                    Codec.BOOL.fieldOf("placed").forGetter(SchematicProjectionData::isPlaced),
                    BlockPos.CODEC.optionalFieldOf("pos").forGetter(SchematicProjectionData::pos),
                    ResourceLocation.CODEC.optionalFieldOf("multiblock").forGetter(SchematicProjectionData::multiblock)
            ).apply(instance, SchematicProjectionData::new));

    public static final StreamCodec<FriendlyByteBuf, Optional<BlockPos>> OPTIONAL_BLOCKPOS_CODEC =
            StreamCodec.of(
                    (buf, opt) -> {
                        buf.writeBoolean(opt.isPresent());
                        opt.ifPresent(pos -> BlockPos.STREAM_CODEC.encode(buf, pos));
                    },
                    buf -> {
                        boolean has = buf.readBoolean();
                        if (!has) return Optional.empty();
                        return Optional.of(BlockPos.STREAM_CODEC.decode(buf));
                    }
            );
    public static final StreamCodec<FriendlyByteBuf, Optional<ResourceLocation>> OPTIONAL_RL_CODEC =
            StreamCodec.of(
                    (buf, opt) -> {
                        buf.writeBoolean(opt.isPresent());
                        opt.ifPresent(rl -> ResourceLocation.STREAM_CODEC.encode(buf, rl));
                    },
                    buf -> {
                        boolean has = buf.readBoolean();
                        if (!has) return Optional.empty();
                        return Optional.of(ResourceLocation.STREAM_CODEC.decode(buf));
                    }
            );

    public static final StreamCodec<FriendlyByteBuf, Rotation> ROTATION_CODEC =
            StreamCodec.of(
                    (buf, r) -> buf.writeInt(r.ordinal()),
                    buf -> Rotation.values()[buf.readInt()]
            );

    public static final StreamCodec<FriendlyByteBuf, ItemStack> ITEMSTACK_CODEC =
            StreamCodec.of(
                    (buf, stack) -> {
                        boolean isEmpty = stack.isEmpty();
                        buf.writeBoolean(!isEmpty);
                        if (!isEmpty) {
                            CompoundTag tag = new CompoundTag();
                            HolderLookup.Provider dummyRegistry = Minecraft.getInstance().level.registryAccess();
                            stack.save(dummyRegistry, tag);
                            buf.writeNbt(tag);
                        }
                    },
                    buf -> {
                        boolean hasStack = buf.readBoolean();
                        if (!hasStack) return ItemStack.EMPTY;

                        CompoundTag tag = buf.readNbt();
                        if (tag == null || tag.isEmpty()) return ItemStack.EMPTY;
                        // No idea if this works yet.
                        HolderLookup.Provider dummyRegistry = Minecraft.getInstance().level.registryAccess();
                        return ItemStack.parseOptional(dummyRegistry, tag);
                    }
            );

    public static final StreamCodec<FriendlyByteBuf, SchematicProjectionData> STREAM_CODEC =
            StreamCodec.composite(
                    ITEMSTACK_CODEC, SchematicProjectionData::formationTool,
                    ROTATION_CODEC, SchematicProjectionData::rotation,
                    ByteBufCodecs.BOOL, SchematicProjectionData::mirror,
                    ByteBufCodecs.BOOL, SchematicProjectionData::isPlaced,
                    OPTIONAL_BLOCKPOS_CODEC, SchematicProjectionData::pos,
                    OPTIONAL_RL_CODEC, SchematicProjectionData::multiblock,
                    SchematicProjectionData::new
            );
}