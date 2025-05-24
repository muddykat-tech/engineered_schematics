package tech.muddykat.engineered_schematics.event;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import tech.muddykat.engineered_schematics.EngineeredSchematics;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class IMCReceiver {
    private static final String TEMPLATE_KEY = "formation_item";

    @SubscribeEvent
    public static void processIMC(final InterModProcessEvent event) {
        InterModComms.getMessages(EngineeredSchematics.MODID).forEach(message -> {
            if (TEMPLATE_KEY.equals(message.method())) {
                Object receivedMessage = message.messageSupplier().get();
                if (receivedMessage instanceof Pair<?, ?> pair) {
                    if (pair.getFirst() instanceof ResourceLocation && pair.getSecond() instanceof ItemStack) {
                        @SuppressWarnings("unchecked")
                        Pair<ResourceLocation, ItemStack> multiblock_name = (Pair<ResourceLocation, ItemStack>) pair;
                        EngineeredSchematics.setTemplateFormationItem(multiblock_name.getFirst(), multiblock_name.getSecond());
                        EngineeredSchematics.LOGGER.info("Received IMC formation item: {} <- {}", multiblock_name.getFirst(), multiblock_name.getSecond().getDisplayName().getString());
                    } else {
                        EngineeredSchematics.LOGGER.warn("Received IMC message with incorrect types: {}", pair);
                    }
                } else {
                    EngineeredSchematics.LOGGER.warn("Received IMC message is not a valid Pair: {}", receivedMessage);
                }
            }
        });
    }
}

