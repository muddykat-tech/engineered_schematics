package tech.muddykat.engineered_schematics.util;

import tech.muddykat.engineered_schematics.EngineeredSchematics;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ESPreviewConfig {
    private static final String CATEGORY = "preview";
    private static final float[] NO_SHIFT = {0.0F, 0.0F};
    private static final Map<String, Float> DEFAULT_SCALES = defaultScales();
    private static final Map<String, float[]> DEFAULT_SHIFTS = defaultShifts();
    private static final Map<String, Float> SCALES = new HashMap<>();
    private static final Map<String, float[]> SHIFTS = new HashMap<>();

    private ESPreviewConfig() {}

    private static Map<String, Float> defaultScales() {
        Map<String, Float> scales = new HashMap<>();
        scales.put("IE:ArcFurnace", 0.75F);
        scales.put("II:AluminiumChainFenceGate", 1.1F);
        scales.put("II:AluminiumFenceGate", 1.1F);
        scales.put("II:ArithmeticLogicMachine", 0.85F);
        scales.put("II:ArtilleryHowitzer", 0.85F);
        scales.put("II:BallisticComputer", 0.7F);
        scales.put("II:DataInputMachine", 0.9F);
        scales.put("II:Electrolyzer", 0.8F);
        scales.put("II:Filler", 0.7F);
        scales.put("II:FuelStation", 0.85F);
        scales.put("II:Packer", 0.85F);
        scales.put("II:ProjectileWorkshop", 0.85F);
        scales.put("II:SkycartStation", 0.7F);
        scales.put("II:SkycrateStation", 0.7F);
        scales.put("II:SteelChainFenceGate", 1.1F);
        scales.put("II:SteelFenceGate", 1.1F);
        scales.put("II:WoodenChainFenceGate", 1.1F);
        scales.put("II:WoodenFenceGate", 1.1F);
        scales.put("IP:DistillationTower", 0.5F);
        scales.put("IT:Alternator", 1.25F);
        scales.put("IT:BoilerLiquid", 0.75F);
        scales.put("IT:BoilerSolid", 0.75F);
        scales.put("IT:BoilerTank", 0.75F);
        scales.put("IT:CoolingTower", 0.65F);
        scales.put("IT:GasTurbine", 1.25F);
        scales.put("IT:SolarMelter", 1.5F);
        scales.put("IT:SolarTower", 1.5F);
        scales.put("IT:electrolyticCrucibleBattery", 0.75F);
        scales.put("IT:meltingCrucible", 1.25F);
        return scales;
    }

    private static Map<String, float[]> defaultShifts() {
        Map<String, float[]> shifts = new HashMap<>();
        shifts.put("IE:BlastFurnace", new float[]{1.0F, 0.0F});
        shifts.put("IE:BottlingMachine", new float[]{-1.0F, 0.0F});
        shifts.put("IE:CokeOven", new float[]{1.0F, 0.0F});
        shifts.put("II:RedstoneDataInterface", new float[]{0.5F, 0.0F});
        shifts.put("IP:DistillationTower", new float[]{-4.0F, -5.0F});
        shifts.put("IP:Pumpjack", new float[]{1.0F, 0.0F});
        shifts.put("IT:AdvancedCokeOven", new float[]{1.0F, 0.0F});
        shifts.put("IT:Alternator", new float[]{1.0F, 0.0F});
        shifts.put("IT:BoilerLiquid", new float[]{-2.0F, -1.0F});
        shifts.put("IT:BoilerSolid", new float[]{-2.0F, -1.0F});
        shifts.put("IT:BoilerTank", new float[]{-1.0F, 0.0F});
        shifts.put("IT:CoolingTower", new float[]{0.0F, 2.0F});
        shifts.put("IT:Radiator", new float[]{2.0F, 0.0F});
        shifts.put("IT:SolarMelter", new float[]{0.0F, -4.0F});
        shifts.put("IT:SolarReflector", new float[]{2.0F, 0.0F});
        shifts.put("IT:SolarTower", new float[]{0.0F, -4.0F});
        shifts.put("IT:SteelSheetmetalTank", new float[]{1.0F, 2.0F});
        shifts.put("IT:electrolyticCrucibleBattery", new float[]{1.0F, 0.0F});
        return shifts;
    }

    public static void sync() {
        Configuration config = new Configuration(new File(Loader.instance().getConfigDir(), EngineeredSchematics.MODID + ".cfg"), true);
        config.setCategoryComment(CATEGORY, "Schematic table preview adjustments, one category per registered multiblock. scale multiplies the preview size; shift_right and shift_down move it on screen in block units, negative values going left and up.");
        for (MultiblockHandler.IMultiblock multiblock : MultiblockHandler.getMultiblocks()) {
            String name = multiblock.getUniqueName();
            String category = CATEGORY + Configuration.CATEGORY_SPLITTER + name;
            float[] fallback = DEFAULT_SHIFTS.getOrDefault(name, NO_SHIFT);
            SCALES.put(name, (float)config.get(category, "scale", DEFAULT_SCALES.getOrDefault(name, 1.0F)).getDouble());
            SHIFTS.put(name, new float[]{(float)config.get(category, "shift_right", fallback[0]).getDouble(),
                    (float)config.get(category, "shift_down", fallback[1]).getDouble()});
        }
        if (config.hasChanged()) { config.save(); }
    }

    public static float getScale(MultiblockHandler.IMultiblock multiblock) { return SCALES.getOrDefault(multiblock.getUniqueName(), 1.0F); }

    public static float[] getShift(MultiblockHandler.IMultiblock multiblock) { return SHIFTS.getOrDefault(multiblock.getUniqueName(), NO_SHIFT); }
}
