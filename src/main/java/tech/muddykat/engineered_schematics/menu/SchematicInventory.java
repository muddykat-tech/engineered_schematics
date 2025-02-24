package tech.muddykat.engineered_schematics.menu;


import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.item.ESSchematicSettings;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import java.util.ArrayList;
import java.util.List;

public class SchematicInventory extends SimpleContainer
{
    private final List<MultiblockHandler.IMultiblock> schematics;
    private final SchematicsContainerMenu menu;
    private static ArrayList<MultiblockHandler.IMultiblock> exceptions = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger("registry");

    public SchematicInventory(SchematicsContainerMenu container, List<MultiblockHandler.IMultiblock> schematics)
    {
        super(schematics.size());
        this.schematics = schematics;
        this.menu = container;
    }

    public void updateOutputs(Container inputInventory)
    {
        //Get input items
        NonNullList<ItemStack> inputs = NonNullList.withSize(inputInventory.getContainerSize()-1, ItemStack.EMPTY);
        for(int i = 0; i < inputs.size(); i++)
            inputs.set(i, inputInventory.getItem(i));
        //Iterate Recipes and set output slots
        if(inputs.get(0).getCount() > 0)
        {
            for(int i = 0; i < this.schematics.size(); i++)
            {
                MultiblockHandler.IMultiblock mb = schematics.get(i);
                if(mb instanceof TemplateMultiblock template)
                {
                    ItemStack schematic = new ItemStack(ESRegistry.SCHEMATIC_ITEM.get());
                    ESSchematicSettings settings = new ESSchematicSettings(schematic);
                    settings.setMultiblock(template);
                    settings.setMirror(this.menu.isMirroredSchematic);
                    settings.setRotation(Rotation.NONE);
                    settings.setPlaced(false);
                    settings.applyTo(schematic);
                    this.setItem(i, schematic.copy());
                }
                else
                {
                    if(!exceptions.contains(mb))
                    {
                        exceptions.add(mb);
                        logger.warn("An IMultiblock for the Schematic Table was not an instance of TemplateMultiblock [{}]", mb.getUniqueName());
                    }
                }
            }
        }
    }

    private NonNullList<ItemStack> consumePaper(NonNullList<ItemStack> query, int crafted)
    {
        if(!query.isEmpty() && query.get(0).getCount() > 0)
        {
            ItemStack paper = query.get(0);
            if(paper.is(Items.PAPER))
            {
                int count = paper.getCount() - crafted;
                if(0 == count) query.set(0, ItemStack.EMPTY);
                if(count > 0){
                    paper.setCount(count);
                    query.set(0, paper.copy());
                }
            } else
            {
                logger.warn("Schematic Table had an input that was not paper?");
            }
        }

        return query;
    }

    public void reduceInputs(Container inputInventory, ItemStack taken)
    {
        //Get input items
        NonNullList<ItemStack> inputs = NonNullList.withSize(inputInventory.getContainerSize()-1, ItemStack.EMPTY);
        for(int i = 0; i < inputs.size(); i++)
            inputs.set(i, inputInventory.getItem(i));
        //Consume

        consumePaper(inputs, 1);

        //Update remains
        for(int i = 0; i < inputs.size(); i++)
            inputInventory.setItem(i, inputs.get(i));

        updateOutputs(inputInventory);
    }
}