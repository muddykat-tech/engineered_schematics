package tech.muddykat.engineered_schematics.block.entity;

import net.minecraft.network.chat.Component;

public enum BoardMode
{
    BUILDING, PLANNING, SIMULATION;

    private final String translation = "desc.engineered_schematics.info.board.mode_" + ordinal();

    public BoardMode next()
    {
        BoardMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }

    public Component getTranslated()
    {
        return Component.translatable(translation);
    }

    public static BoardMode byOrdinal(int ordinal)
    {
        BoardMode[] modes = values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : BUILDING;
    }
}
