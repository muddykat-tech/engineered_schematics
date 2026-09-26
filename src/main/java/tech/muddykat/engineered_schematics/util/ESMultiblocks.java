package tech.muddykat.engineered_schematics.util;

import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.item.SchematicProjection;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ESMultiblocks {
    public static final String KEY_NAME = "desc." + EngineeredSchematics.MODID + ".multiblock.";

    private static final String METHOD_TRIGGER = "primaryTrigger";
    private static final String IC_REGISTRY = "com.immersiveconvergence.common.multiblock.IEMultiblockRegistry";
    private static final String IC_API_REGISTRY = "com.immersiveconvergence.api.multiblock.MultiblockRegistry";
    private static final String IC_TEMPLATE = "com.immersiveconvergence.api.multiblock.TemplateMultiblock";
    private static final BlockPos NO_TRIGGER = new BlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    private static final Map<String, BlockPos> TRIGGERS = triggers();
    private static final Map<String, BlockPos> RESOLVED = new HashMap<>();
    private static final Map<String, Object> IC_OBJECTS = new HashMap<>();
    private static final Set<String> REVERSED_LENGTH = new HashSet<>(Arrays.asList("IE:Crusher", "IE:Squeezer", "IE:Fermenter", "IE:Mixer",
            "IE:Refinery", "IE:DieselGenerator", "IE:ArcFurnace", "IE:Excavator", "IE:AutoWorkbench", "IE:BottlingMachine", "IP:Pumpjack"));

    private ESMultiblocks() {}

    private static Map<String, BlockPos> triggers() {
        Map<String, BlockPos> triggers = new HashMap<>();
        triggers.put("IE:AlloySmelter", new BlockPos(1, 1, 1));
        triggers.put("IE:ArcFurnace", new BlockPos(2, 0, 4));
        triggers.put("IE:Assembler", new BlockPos(1, 1, 2));
        triggers.put("IE:AutoWorkbench", new BlockPos(1, 1, 2));
        triggers.put("IE:BlastFurnace", new BlockPos(1, 1, 2));
        triggers.put("IE:BlastFurnaceAdvanced", new BlockPos(1, 1, 2));
        triggers.put("IE:BottlingMachine", new BlockPos(1, 1, 1));
        triggers.put("IE:BucketWheel", new BlockPos(0, 3, 3));
        triggers.put("IE:CokeOven", new BlockPos(1, 1, 2));
        triggers.put("IE:Crusher", new BlockPos(2, 1, 2));
        triggers.put("IE:DieselGenerator", new BlockPos(1, 1, 4));
        triggers.put("IE:Excavator", new BlockPos(1, 1, 5));
        triggers.put("IE:Fermenter", new BlockPos(1, 1, 1));
        triggers.put("IE:Lightningrod", new BlockPos(1, 1, 2));
        triggers.put("IE:MetalPress", new BlockPos(0, 1, 1));
        triggers.put("IE:Mixer", new BlockPos(1, 1, 1));
        triggers.put("IE:Refinery", new BlockPos(2, 1, 2));
        triggers.put("IE:SheetmetalTank", new BlockPos(1, 1, 2));
        triggers.put("IE:Silo", new BlockPos(1, 1, 2));
        triggers.put("IE:Squeezer", new BlockPos(1, 1, 1));
        triggers.put("IP:DistillationTower", new BlockPos(0, 1, 3));
        triggers.put("IP:Pumpjack", new BlockPos(1, 1, 4));
        return triggers;
    }

    public static boolean hasReversedLength(MultiblockHandler.IMultiblock multiblock) { return REVERSED_LENGTH.contains(multiblock.getUniqueName()) || isTemplateMultiblock(multiblock); }

    private static boolean isTemplateMultiblock(MultiblockHandler.IMultiblock multiblock) {
        if (walksToTemplate(multiblock)) { return true; }
        return walksToTemplate(fromConvergenceApi(multiblock.getUniqueName()));
    }

    private static boolean walksToTemplate(@Nullable Object multiblock) {
        if (multiblock == null) { return false; }
        for (Class<?> type = multiblock.getClass(); type != null; type = type.getSuperclass()) {
            if (IC_TEMPLATE.equals(type.getName())) { return true; }
        }
        return false;
    }

    @Nullable
    private static Object fromConvergenceApi(String uniqueName) {
        Object cached = IC_OBJECTS.get(uniqueName);
        if (cached != null) { return cached == IC_OBJECTS ? null : cached; }
        Object found = findInConvergenceApi(uniqueName);
        IC_OBJECTS.put(uniqueName, found == null ? IC_OBJECTS : found);
        return found;
    }

    @Nullable
    private static Object findInConvergenceApi(String uniqueName) {
        try {
            Object list = Class.forName(IC_API_REGISTRY).getMethod("getMultiblocks").invoke(null);
            if (!(list instanceof Iterable)) { return null; }
            for (Object candidate : (Iterable<?>)list) {
                Object name = candidate.getClass().getMethod("getUniqueName").invoke(candidate);
                if (uniqueName.equals(name)) { return candidate; }
            }
            return null;
        }
        catch (ReflectiveOperationException | LinkageError exception) { return null; }
    }

    @Nullable
    public static BlockPos getTriggerOffset(MultiblockHandler.IMultiblock multiblock) {
        String uniqueName = multiblock.getUniqueName();
        BlockPos resolved = RESOLVED.get(uniqueName);
        if (resolved == null) {
            resolved = toCanonical(multiblock, fromConvergence(uniqueName));
            if (resolved == null) { resolved = toCanonical(multiblock, readTriggerField(fromConvergenceApi(uniqueName))); }
            if (resolved == null) { resolved = TRIGGERS.get(uniqueName); }
            if (resolved == null) { resolved = toCanonical(multiblock, readTriggerField(multiblock)); }
            RESOLVED.put(uniqueName, resolved == null ? NO_TRIGGER : resolved);
        }
        return resolved == NO_TRIGGER ? null : resolved;
    }

    @Nullable
    private static BlockPos toCanonical(MultiblockHandler.IMultiblock multiblock, @Nullable BlockPos template) {
        if (template == null || !hasReversedLength(multiblock)) { return template; }
        int length = SchematicProjection.getSize(multiblock).getZ();
        return length > 0 ? new BlockPos(template.getX(), template.getY(), length - 1 - template.getZ()) : template;
    }

    @Nullable
    private static BlockPos fromConvergence(String uniqueName) {
        try {
            Object multiblock = Class.forName(IC_REGISTRY).getMethod("get", String.class).invoke(null, uniqueName);
            if (multiblock == null) { return null; }
            Object value = multiblock.getClass().getMethod(METHOD_TRIGGER).invoke(multiblock);
            return value instanceof BlockPos ? (BlockPos)value : null;
        }
        catch (ReflectiveOperationException | LinkageError exception) { return null; }
    }

    @Nullable
    private static BlockPos readTriggerField(@Nullable Object multiblock) {
        if (multiblock == null) { return null; }
        try {
            Object value = multiblock.getClass().getMethod(METHOD_TRIGGER).invoke(multiblock);
            return value instanceof BlockPos ? (BlockPos)value : null;
        }
        catch (ReflectiveOperationException exception) { return null; }
    }

    @Nullable
    public static MultiblockHandler.IMultiblock getByUniqueName(String uniqueName) {
        for (MultiblockHandler.IMultiblock candidate : MultiblockHandler.getMultiblocks()) {
            if (candidate.getUniqueName().equals(uniqueName)) { return candidate; }
        }
        return null;
    }

    public static String getDisplayName(MultiblockHandler.IMultiblock multiblock) { return ESLang.translate(KEY_NAME + multiblock.getUniqueName()); }
}
