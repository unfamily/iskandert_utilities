package net.unfamily.iskautils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.model.data.ModelData;
import net.unfamily.iskautils.events.SetWrenchDirectionBlock.RotationMode;
import net.unfamily.iskautils.network.packet.SwissWrenchApplyModeC2SPacket;
import net.unfamily.iskautils.network.packet.SwissWrenchRadialSubmitC2SPacket;
import net.unfamily.iskautils.util.SwissWrenchRotationProperties;
import net.unfamily.iskautils.util.SwissWrenchRotationProperties.RotationStep;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Nested orientation picker for Swiss Wrench (sector ring + quick-rotate buttons, 26.x).
 * Center ← / → buttons apply legacy rotate without closing.
 */
public class SwissWrenchRadialScreen extends Screen {

    private static final int INNER_RADIUS = 48;
    private static final int OUTER_RADIUS = 118;
    private static final int SECTOR_HALF = 22;
    private static final float PREVIEW_SCALE = 24.0f;
    /**
     * Entity GUI path: player yaw only (+yaw mirrored). Do not use the +180 PoseStack offset from 1.21.1.
     */
    private static final float PREVIEW_YAW_OFFSET = 0.0f;
    /** Packed block+sky light max (legacy LightTexture.FULL_BRIGHT). */
    private static final int FULL_BRIGHT = 0xF000F0;

    private static final int PAD_BTN = 24;
    private static final int DONE_BTN_W = 36;
    private static final int PAD_GAP = 6;

    private static final RotationMode[] QUICK_PAD_MODES = {
            RotationMode.ROTATE_LEFT,
            RotationMode.ROTATE_RIGHT
    };

    private final BlockPos pos;
    private BlockState originalState;
    private List<RotationStep> steps;

    /** Composed state after completed steps (before current step choice). */
    private BlockState pendingState;
    private int stepIndex;
    private final List<String> chosenPropertyNames = new ArrayList<>();
    private final List<String> chosenValueNames = new ArrayList<>();

    private List<BlockState> valueStates = List.of();
    private int selectedValueIndex;
    private RotationMode hoveredPadMode = null;
    private boolean hoveredDone = false;

    public SwissWrenchRadialScreen(BlockPos pos, BlockState state, List<RotationStep> steps) {
        super(Component.translatable("item.iska_utils.swiss_wrench.radial.title"));
        this.pos = pos;
        this.originalState = state;
        this.steps = steps;
        this.pendingState = state;
        this.stepIndex = 0;
        rebuildValues();
    }

    public static void tryOpen(BlockPos pos, BlockState state) {
        List<RotationStep> steps = SwissWrenchRotationProperties.buildSteps(state);
        if (steps.isEmpty()) {
            return;
        }
        Minecraft.getInstance().setScreen(new SwissWrenchRadialScreen(pos, state, steps));
    }

    private RotationStep currentStep() {
        return steps.get(stepIndex);
    }

    private Property<?> currentProperty() {
        return SwissWrenchRotationProperties.propertyForStep(originalState, currentStep());
    }

    private void rebuildValues() {
        Property<?> property = currentProperty();
        if (property == null) {
            valueStates = List.of();
            selectedValueIndex = 0;
            return;
        }
        valueStates = SwissWrenchRotationProperties.cycleValues(pendingState, property);
        selectedValueIndex = 0;
        for (int i = 0; i < valueStates.size(); i++) {
            if (valueStates.get(i).getValue(property).equals(pendingState.getValue(property))) {
                selectedValueIndex = i;
                break;
            }
        }
    }

    /** Reset nested flow from the live block state after a quick-rotate. */
    private void rebindFromState(BlockState state) {
        List<RotationStep> newSteps = SwissWrenchRotationProperties.buildSteps(state);
        if (newSteps.isEmpty()) {
            onClose();
            return;
        }
        originalState = state;
        steps = newSteps;
        pendingState = state;
        stepIndex = 0;
        chosenPropertyNames.clear();
        chosenValueNames.clear();
        rebuildValues();
    }

    private int padOriginX() {
        int totalW = PAD_BTN + PAD_GAP + DONE_BTN_W + PAD_GAP + PAD_BTN;
        return this.width / 2 - totalW / 2;
    }

    private int padOriginY() {
        int cy = this.height / 2;
        return steps.size() > 1 ? cy + 18 : cy + 8;
    }

    private int padButtonX(RotationMode mode) {
        int ox = padOriginX();
        return mode == RotationMode.ROTATE_RIGHT
                ? ox + PAD_BTN + PAD_GAP + DONE_BTN_W + PAD_GAP
                : ox;
    }

    private int doneButtonX() {
        return padOriginX() + PAD_BTN + PAD_GAP;
    }

    private RotationMode padModeAt(double mouseX, double mouseY) {
        int y = padOriginY();
        for (RotationMode mode : QUICK_PAD_MODES) {
            int x = padButtonX(mode);
            if (mouseX >= x && mouseX < x + PAD_BTN && mouseY >= y && mouseY < y + PAD_BTN) {
                return mode;
            }
        }
        return null;
    }

    private boolean doneAt(double mouseX, double mouseY) {
        int x = doneButtonX();
        int y = padOriginY();
        return mouseX >= x && mouseX < x + DONE_BTN_W && mouseY >= y && mouseY < y + PAD_BTN;
    }

    private void sendQuickRotate(RotationMode mode) {
        ClientPacketDistributor.sendToServer(SwissWrenchApplyModeC2SPacket.radialQuick(pos, mode));
    }

    private void confirmCurrentStep() {
        if (selectedValueIndex >= 0 && selectedValueIndex < valueStates.size()) {
            advance(selectedValueIndex);
        }
    }

    private static String padGlyph(RotationMode mode) {
        return switch (mode) {
            case ROTATE_LEFT -> "\u2190";
            case ROTATE_RIGHT -> "\u2192";
            default -> "?";
        };
    }

    private static Component padTooltip(RotationMode mode) {
        return switch (mode) {
            case ROTATE_LEFT -> Component.translatable("item.iska_utils.swiss_wrench.radial.pad.left");
            case ROTATE_RIGHT -> Component.translatable("item.iska_utils.swiss_wrench.radial.pad.right");
            default -> Component.empty();
        };
    }

    private void renderQuickPad(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int y = padOriginY();
        for (RotationMode mode : QUICK_PAD_MODES) {
            int x = padButtonX(mode);
            boolean hot = mode == hoveredPadMode;
            graphics.fill(x, y, x + PAD_BTN, y + PAD_BTN, hot ? 0xAA55AAFF : 0xCC222222);
            graphics.fill(x + 1, y + 1, x + PAD_BTN - 1, y + PAD_BTN - 1, hot ? 0x664488CC : 0x44111111);
            graphics.centeredText(this.font, padGlyph(mode), x + PAD_BTN / 2, y + (PAD_BTN - 8) / 2,
                    hot ? 0xFFFFFFFF : 0xFFDDDDDD);
        }

        int dx = doneButtonX();
        boolean doneHot = hoveredDone;
        graphics.fill(dx, y, dx + DONE_BTN_W, y + PAD_BTN, doneHot ? 0xAA55AAFF : 0xCC222222);
        graphics.fill(dx + 1, y + 1, dx + DONE_BTN_W - 1, y + PAD_BTN - 1, doneHot ? 0x664488CC : 0x44111111);
        graphics.centeredText(this.font,
                Component.translatable("item.iska_utils.swiss_wrench.radial.pad.done"),
                dx + DONE_BTN_W / 2, y + (PAD_BTN - 8) / 2, doneHot ? 0xFFFFFFFF : 0xFFDDDDDD);

        if (hoveredPadMode != null) {
            graphics.setTooltipForNextFrame(this.font, padTooltip(hoveredPadMode), mouseX, mouseY);
        } else if (hoveredDone) {
            graphics.setTooltipForNextFrame(this.font,
                    Component.translatable("item.iska_utils.swiss_wrench.radial.pad.done_tooltip"),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the world visible behind the radial; do not dim the screen.
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.level == null) {
            return;
        }
        BlockState live = minecraft.level.getBlockState(pos);
        if (!live.is(originalState.getBlock())) {
            onClose();
            return;
        }
        if (!live.equals(originalState)) {
            rebindFromState(live);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int cy = this.height / 2;

        hoveredPadMode = padModeAt(mouseX, mouseY);
        hoveredDone = hoveredPadMode == null && doneAt(mouseX, mouseY);

        Property<?> property = currentProperty();
        int count = valueStates.size();
        if (property != null && count > 0) {
            double ring = (INNER_RADIUS + OUTER_RADIUS) / 2.0;
            for (int i = 0; i < count; i++) {
                double mid = sectorAngle(i, count);
                int ix = cx + (int) (Math.cos(mid) * ring);
                int iy = cy + (int) (Math.sin(mid) * ring);

                boolean hot = i == selectedValueIndex;
                int color = hot ? 0xAA55AAFF : 0x88222222;
                graphics.fill(ix - SECTOR_HALF, iy - SECTOR_HALF, ix + SECTOR_HALF, iy + SECTOR_HALF, color);

                renderBlockStatePreview(graphics, valueStates.get(i), ix, iy);

                Component label = SwissWrenchRotationProperties.sectorLabel(property, valueStates.get(i), pendingState);
                graphics.centeredText(this.font, label, ix, iy + SECTOR_HALF + 2, hot ? 0xFFFFFFFF : 0xFFCCCCCC);
            }

            graphics.centeredText(this.font, currentStep().title(), cx, cy - 10, 0xFFFFFFFF);
            if (steps.size() > 1) {
                graphics.centeredText(this.font,
                        Component.translatable("item.iska_utils.swiss_wrench.radial.step_progress",
                                stepIndex + 1, steps.size()),
                        cx, cy + 4, 0xFFAAAAAA);
            }
        }

        renderQuickPad(graphics, mouseX, mouseY);
    }

    private void renderBlockStatePreview(GuiGraphicsExtractor graphics, BlockState blockState, int centerX, int centerY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        float yaw = mc.player != null ? mc.player.getYRot() : 0.0f;

        FallingBlockRenderState renderState = new FallingBlockRenderState();
        renderState.entityType = EntityType.FALLING_BLOCK;
        renderState.boundingBoxWidth = 1.0f;
        renderState.boundingBoxHeight = 1.0f;
        renderState.lightCoords = FULL_BRIGHT;

        MovingBlockRenderState moving = renderState.movingBlockRenderState;
        moving.blockState = blockState;
        moving.blockPos = BlockPos.ZERO;
        moving.randomSeedPos = BlockPos.ZERO;
        moving.cardinalLighting = CardinalLighting.DEFAULT;
        moving.biome = mc.level.getBiome(pos);
        moving.lightEngine = mc.level.getLightEngine();
        moving.modelData = ModelData.EMPTY;

        // Slight high-side view: +X after rotateZ(PI) reads as from below; use negative pitch instead.
        Quaternionf rotation = new Quaternionf()
                .rotateZ((float) Math.PI)
                .rotateX((float) Math.toRadians(-25.0))
                .rotateY((float) Math.toRadians(PREVIEW_YAW_OFFSET + yaw));
        Vector3f translation = new Vector3f(0.0f, 0.5f, 0.0f);

        graphics.entity(
                renderState,
                PREVIEW_SCALE,
                translation,
                rotation,
                null,
                centerX - SECTOR_HALF,
                centerY - SECTOR_HALF,
                centerX + SECTOR_HALF,
                centerY + SECTOR_HALF);
    }

    private static double sectorAngle(int index, int count) {
        if (count == 4) {
            return -Math.PI / 2.0 + index * (Math.PI / 2.0);
        }
        double slice = (Math.PI * 2.0) / count;
        return -Math.PI / 2.0 + slice * index + slice / 2.0;
    }

    private int sectorIndexAt(double mouseX, double mouseY) {
        int count = valueStates.size();
        if (count == 0) {
            return -1;
        }
        int cx = this.width / 2;
        int cy = this.height / 2;
        double ring = (INNER_RADIUS + OUTER_RADIUS) / 2.0;
        for (int i = 0; i < count; i++) {
            double mid = sectorAngle(i, count);
            int ix = cx + (int) (Math.cos(mid) * ring);
            int iy = cy + (int) (Math.sin(mid) * ring);
            if (mouseX >= ix - SECTOR_HALF && mouseX < ix + SECTOR_HALF
                    && mouseY >= iy - SECTOR_HALF && mouseY < iy + SECTOR_HALF) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 1) {
            return goBack();
        }
        if (event.button() == 0) {
            RotationMode pad = padModeAt(mouseX, mouseY);
            if (pad != null) {
                sendQuickRotate(pad);
                return true;
            }
            if (doneAt(mouseX, mouseY)) {
                confirmCurrentStep();
                return true;
            }
            int sector = sectorIndexAt(mouseX, mouseY);
            if (sector >= 0) {
                advance(sector);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        RotationMode quick = null;
        if (minecraft != null) {
            if (minecraft.options.keyLeft.matches(event) || event.key() == GLFW.GLFW_KEY_LEFT) {
                quick = RotationMode.ROTATE_LEFT;
            } else if (minecraft.options.keyRight.matches(event) || event.key() == GLFW.GLFW_KEY_RIGHT) {
                quick = RotationMode.ROTATE_RIGHT;
            }
        } else if (event.key() == GLFW.GLFW_KEY_LEFT) {
            quick = RotationMode.ROTATE_LEFT;
        } else if (event.key() == GLFW.GLFW_KEY_RIGHT) {
            quick = RotationMode.ROTATE_RIGHT;
        }
        if (quick != null) {
            sendQuickRotate(quick);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (stepIndex > 0) {
                goBack();
                return true;
            }
            return super.keyPressed(event);
        }
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE || event.key() == GLFW.GLFW_KEY_DELETE) {
            return goBack();
        }
        if (event.isConfirmation()) {
            confirmCurrentStep();
            return true;
        }
        return super.keyPressed(event);
    }

    private boolean goBack() {
        if (stepIndex <= 0) {
            onClose();
            return true;
        }
        stepIndex--;
        if (!chosenPropertyNames.isEmpty()) {
            chosenPropertyNames.remove(chosenPropertyNames.size() - 1);
            chosenValueNames.remove(chosenValueNames.size() - 1);
        }
        pendingState = originalState;
        for (int i = 0; i < chosenPropertyNames.size(); i++) {
            var applied = SwissWrenchRotationProperties.applyNamedValue(
                    pendingState, chosenPropertyNames.get(i), chosenValueNames.get(i));
            if (applied.isPresent()) {
                pendingState = applied.get();
            }
        }
        rebuildValues();
        return true;
    }

    private void advance(int index) {
        Property<?> property = currentProperty();
        if (property == null || index < 0 || index >= valueStates.size()) {
            return;
        }
        BlockState chosen = valueStates.get(index);
        String propertyName = currentStep().propertyName();
        String valueName = SwissWrenchRotationProperties.valueName(property, chosen);

        chosenPropertyNames.add(propertyName);
        chosenValueNames.add(valueName);
        pendingState = chosen;

        if (stepIndex + 1 < steps.size()) {
            stepIndex++;
            rebuildValues();
            return;
        }

        ClientPacketDistributor.sendToServer(new SwissWrenchRadialSubmitC2SPacket(
                pos,
                List.copyOf(chosenPropertyNames),
                List.copyOf(chosenValueNames)));
        onClose();
    }
}
