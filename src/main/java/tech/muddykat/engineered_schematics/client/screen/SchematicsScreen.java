package tech.muddykat.engineered_schematics.client.screen;

import blusunrize.immersiveengineering.api.multiblocks.ClientMultiblocks;
import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import blusunrize.immersiveengineering.client.gui.IEContainerScreen;
import blusunrize.immersiveengineering.client.gui.elements.GuiButtonCheckbox;
import blusunrize.immersiveengineering.client.gui.elements.GuiButtonState;
import blusunrize.immersiveengineering.client.gui.info.InfoArea;
import blusunrize.lib.manual.ManualUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.client.gui.widget.ScrollPanel;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.block.entity.SchematicTableBlockEntity;
import tech.muddykat.engineered_schematics.menu.SchematicsContainerMenu;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class SchematicsScreen extends IEContainerScreen<SchematicsContainerMenu>
{
    private static final ResourceLocation TEXTURE = EngineeredSchematics.makeTextureLocation("schematic_gui");
    private static final int MAX_DISPLAY_WIDTH = 80;
    private static final int TEXT_COLOR = 0x666666;

    private GuiButtonCheckbox mirrorSchematicBtn;
    private SchematicScrollPanel selectionPanel;
    private float scrollAmount;

    public SchematicsScreen(SchematicsContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE);
        this.imageHeight = 218;
        this.imageWidth = 230;
    }

    @Override
    protected void init() {
        super.init();

        // Initialize label positions
        this.titleLabelY = 9;
        this.titleLabelX = 14;
        this.inventoryLabelX = 36;
        this.inventoryLabelY = 125;

        initMirrorButton();
        initSelectionPanel();
    }

    private void initMirrorButton() {
        this.mirrorSchematicBtn = this.addRenderableWidget(
                new GuiButtonCheckbox(
                        leftPos + 129,
                        topPos + 112,
                        Component.translatable("engineered_schematics.gui.schematic_table.mirror"),
                        () -> menu.isMirroredSchematic,
                        this::handleMirrorButton
                )
        );
    }

    private void handleMirrorButton(GuiButtonState<Boolean> btn) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("mirrored", !btn.getState());
        handleMirrorButtonClick(tag);
    }

    private void initSelectionPanel() {
        this.selectionPanel = this.addRenderableWidget(
                new SchematicScrollPanel(
                        minecraft,
                        110,
                        92,
                        topPos + 20,
                        leftPos + 9,
                        menu.availableMultiblocks,
                        this::handleSchematicSelection,
                        (index) -> index == menu.selected_schematic,
                        (amount) -> {
                            scrollAmount = amount;
                            return true;
                        }
                )
        );

        this.selectionPanel.setScrollAmount(scrollAmount);
    }

    private boolean handleSchematicSelection(int index) {
        this.minecraft.tell(() -> menu.jumpToSchematic(index));

        CompoundTag nbt = new CompoundTag();
        nbt.putInt("index", index);
        sendUpdateToServer(nbt);

        return true;
    }

    @Override
    protected void drawBackgroundTexture(GuiGraphics graphics) {
        // Draw base texture
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Return early if no multiblocks available
        if (menu.availableMultiblocks.isEmpty()) {
            return;
        }

        TemplateMultiblock mb = menu.availableMultiblocks.get(menu.selected_schematic);
        PoseStack pose = graphics.pose();

        drawMultiblockName(graphics, pose, mb);

        pose.pushPose();
        renderManualMultimodel(pose, graphics, mb);
        pose.popPose();
    }

    private void drawMultiblockName(GuiGraphics graphics, PoseStack pose, TemplateMultiblock mb) {
        float textWidth = this.font.width(mb.getDisplayName().getVisualOrderText());
        float scale = textWidth > MAX_DISPLAY_WIDTH ? (MAX_DISPLAY_WIDTH / textWidth) : 1.0f;

        pose.pushPose();
        pose.translate(leftPos + 174.5, topPos + (8.5*((2+scale)-(scale*2))), 0);
        pose.scale(scale, scale, scale);
        graphics.drawString(
                this.font,
                mb.getDisplayName(),
                -this.font.width(mb.getDisplayName().getVisualOrderText()) / 2,
                0,
                TEXT_COLOR,
                false
        );
        pose.popPose();
    }

    public void renderManualMultimodel(PoseStack pose, GuiGraphics graphics, TemplateMultiblock mb) {
        SchematicTableBlockEntity tile = getMenu().tile;
        if (tile == null) return;
        Level level = tile.getLevel();
        if(level == null) return;
        ClientMultiblocks.MultiblockManualData renderProperties = ClientMultiblocks.get(mb);
        List<StructureTemplate.StructureBlockInfo> structure = mb.getStructure(level);

        int[] structureDimensions = calculateStructureDimensions(structure);
        int structureHeight = structureDimensions[0];
        int structureWidth = structureDimensions[1];
        int structureLength = structureDimensions[2];

        float maxDimension = Math.max(structureHeight, Math.max(structureWidth, structureLength));

        pose.pushPose();

        // Set up the rendering transformation
        float scale = mb.getManualScale() * 0.65f;
        float renderPosX = leftPos + 174.5f;
        float renderPosY = topPos + 54.5f;

        pose.translate(renderPosX, renderPosY, maxDimension);
        pose.scale(scale, -scale, 1.0F);

        // Apply rotations
        Transformation additionalTransform = createRenderTransform();
        pose.pushTransformation(additionalTransform);
        pose.mulPose((new Quaternionf()).rotateXYZ(0.0F, 1.5707964F, 0.0F));

        // Center the structure
        pose.translate(
                (float)structureLength / -2.0F,
                (float)structureHeight / -2.0F,
                (float)structureWidth / -2.0F
        );

        // Render the formed structure if possible
        if (renderProperties.canRenderFormedStructure()) {
            pose.pushPose();
            renderProperties.renderFormedStructure(pose, graphics.bufferSource());
            pose.popPose();
        }

        pose.popPose();
    }

    private int[] calculateStructureDimensions(List<StructureTemplate.StructureBlockInfo> structure) {
        int structureHeight = 0;
        int structureWidth = 0;
        int structureLength = 0;

        for (StructureTemplate.StructureBlockInfo block : structure) {
            structureHeight = Math.max(structureHeight, block.pos().getY() + 1);
            structureWidth = Math.max(structureWidth, block.pos().getZ() + 1);
            structureLength = Math.max(structureLength, block.pos().getX() + 1);
        }

        return new int[] { structureHeight, structureWidth, structureLength };
    }

    private Transformation createRenderTransform() {
        return new Transformation(
                null,
                (new Quaternionf()).rotateXYZ((float) Math.toRadians(25.0), 0.0F, 0.0F),
                null,
                (new Quaternionf()).rotateXYZ(0.0F, (float) Math.toRadians(-45.0), 0.0F)
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        // Sometimes we get a Concurrent Modification error, this should prevent it from being an issue.
        try
        {
            super.render(graphics, mouseX, mouseY, partialTicks);
        } catch(Exception ignored){};
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x666666, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x666666, false);
    }

    @Override
    protected void drawContainerBackgroundPre(@Nonnull GuiGraphics graphics, float f, int mx, int my)
    {

    }

    private void handleMirrorButtonClick(CompoundTag nbt)
    {
        if(!nbt.isEmpty())
        {
            sendUpdateToServer(nbt);
            getMenu().flipSchematic();
        }
    }

    @NotNull
    @Override
    protected List<InfoArea> makeInfoAreas()
    {
        List<InfoArea> areas = new ArrayList<>();
        return areas;
    }

    public static class SchematicScrollPanel extends ScrollPanel
    {
        private final List<TemplateMultiblock> schematics;
        private final int entryHeight;
        private final Minecraft client;
        private final Function<Integer, Boolean> callback;
        private final Function<Integer, Boolean> isSelectedCallback;
        private final Function<Float, Boolean> updateScroller;
        public SchematicScrollPanel(Minecraft client, int width, int height, int top, int left, List<TemplateMultiblock> schematics, Function<Integer, Boolean> select_callback, Function<Integer, Boolean> isSelectedCallback, Function<Float, Boolean> updateScroller)
        {
            super(client, width, height, top, left);
            this.schematics = schematics;
            this.entryHeight = 20;
            this.client = client;
            this.callback = select_callback;
            this.isSelectedCallback = isSelectedCallback;
            this.updateScroller = updateScroller;
        }

        public void setScrollAmount(float scroll)
        {
            this.scrollDistance = scroll;
        }

        @Override
        protected int getContentHeight()
        {
            return schematics.size() * (entryHeight+1);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button)
        {
            if(mouseX < left || mouseY < top || mouseX > (left+width) || mouseY > (top + height)) return false;

            int y = (int)(mouseY - top + scrollDistance);
            int index = y / (entryHeight+1);
            if(index >= 0 && index < schematics.size()) {
                updateScroller.apply(scrollDistance);
                return callback.apply(index);
            }
            return false;
        }

        @Override
        protected void drawPanel(GuiGraphics graphics, int entryRight, int scrollY, Tesselator tesselator, int mouseX, int mouseY)
        {
            int y = scrollY - 2;
            PoseStack pose = graphics.pose();
            Font font = client.font;
            for (int i = 0; i < schematics.size(); i++)
            {
                TemplateMultiblock template = schematics.get(i);
                float textWidth = font.width(template.getDisplayName().getVisualOrderText());
                float fScale = textWidth > MAX_DISPLAY_WIDTH ? (MAX_DISPLAY_WIDTH / textWidth) : 1.0f;
                int entryTop = y+i*(entryHeight + 1);
                int entryBottom = entryTop+entryHeight;
                graphics.blit(TEXTURE, left, entryTop, 1, 219, 104, 20);
                boolean hover = mouseY > entryTop && mouseY < entryBottom && isMouseOver(mouseX, mouseY);
                int color = isSelectedCallback.apply(i) ? 0xFFFFFF : (hover ? 0xAAFFAA : 0x666666);

                pose.pushPose();
                pose.translate(left+10, entryTop+(6*((2+fScale)-(fScale*2))),0);
                pose.pushPose();
                    pose.scale(fScale,fScale,fScale);
                    graphics.drawString(font, template.getDisplayName(), 0,0, color, false);
                pose.popPose();
                pose.popPose();
            }
        }

        @Override
        public NarrationPriority narrationPriority()
        {
            return NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {}
    }
}