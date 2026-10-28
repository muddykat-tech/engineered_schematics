package tech.muddykat.engineered_schematics.client;

import blusunrize.immersiveengineering.api.multiblocks.ClientMultiblocks;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Client-only tooltip lines for schematics. Kept out of SchematicItem so a dedicated server never loads the client
 * classes these need when it loads the item.
 */
public class SchematicTooltip
{
    public static void addMaterials(MultiblockHandler.IMultiblock mb, List<Component> tooltip)
    {
        if(!Screen.hasShiftDown())
        {
            tooltip.add(Component.translatable("desc.engineered_schematics.info.schematic.block_info", Component.keybind("shift").withStyle(ChatFormatting.GOLD)));
            return;
        }
        tooltip.add(Component.translatable("desc.engineered_schematics.info.schematic.materials"));
        LocalPlayer player = Minecraft.getInstance().player;
        for(ItemStack material : ClientMultiblocks.get(mb).getTotalMaterials())
        {
            int held = countInInventory(player, material);
            // Red for none carried, gold (Minecraft's orange) for some, green for enough
            ChatFormatting colour = held >= material.getCount() ? ChatFormatting.GREEN : (held > 0 ? ChatFormatting.GOLD : ChatFormatting.RED);
            tooltip.add(Component.literal(" " + material.getCount() + "x ").append(material.getHoverName()).withStyle(colour));
        }
    }

    private static int countInInventory(@Nullable LocalPlayer player, ItemStack material)
    {
        if(player == null) return 0;
        Inventory inventory = player.getInventory();
        int count = 0;
        for(int i = 0; i < inventory.getContainerSize(); i++)
        {
            ItemStack stack = inventory.getItem(i);
            if(ItemStack.isSameItem(stack, material))
                count += stack.getCount();
        }
        return count;
    }
}
