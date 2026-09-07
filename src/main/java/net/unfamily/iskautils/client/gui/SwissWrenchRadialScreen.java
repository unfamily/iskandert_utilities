package net.unfamily.iskautils.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.iskautils.events.SetWrenchDirectionBlock.RotationMode;
import net.unfamily.iskautils.network.packet.SwissWrenchApplyModeC2SPacket;
import net.unfamily.iskautils.network.packet.SwissWrenchRadialSubmitC2SPacket;
import net.unfamily.iskautils.util.SwissWrenchRotationProperties;
import net.unfamily.iskautils.util.SwissWrenchRotationProperties.RotationStep;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Nested orientation picker for Swiss Wrench (sector ring + quick-rotate buttons).
 * Center \u2190 / \u2192 buttons apply legacy rotate without closing.
 */
public class SwissWrenchRadialScreen extends Screen {

    private static final int INNER_RADIUS = 48;
    private static final int OUTER_RADIUS = 118;
    private static final int SECTOR_HALF = 22;
    private static final float PREVIEW_SCALE = 24.0f;
    /** Extra Y offset so camera looks toward the face the player sees in-world. */
    private static final float PREVIEW_YAW_OFFSET = 180.0f;

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

    /** Top-left of the centered hub: ← | Done | → */
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
        PacketDistributor.sendToServer(SwissWrenchApplyModeC2SPacket.radialQuick(pos, mode));
    }

    /** Keep current step value and go to next step / submit. */
    private void confirmCurrentStep() {
        if (selectedValueIndex >= 0 && selectedValueIndex < valueStates.size()) {
            advance(selectedValueIndex);
        }
    }

    private static String padGlyph(RotationMode mode) {
        return switch (mode) {
            case ROTATE_LEFT -> "\u2190"; // ←
            case ROTATE_RIGHT -> "\u2192"; // →
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

    private void renderQuickPad(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = padOriginY();
        for (RotationMode mode : QUICK_PAD_MODES) {
            int x = padButtonX(mode);
            boolean hot = mode == hoveredPadMode;
            graphics.fill(x, y, x + PAD_BTN, y + PAD_BTN, hot ? 0xAA55AAFF : 0xCC222222);
            graphics.fill(x + 1, y + 1, x + PAD_BTN - 1, y + PAD_BTN - 1, hot ? 0x664488CC : 0x44111111);
            graphics.drawCenteredString(this.font, padGlyph(mode), x + PAD_BTN / 2, y + (PAD_BTN - 8) / 2,
                    hot ? 0xFFFFFF : 0xDDDDDD);
        }

        int dx = doneButtonX();
        boolean doneHot = hoveredDone;
        graphics.fill(dx, y, dx + DONE_BTN_W, y + PAD_BTN, doneHot ? 0xAA55AAFF : 0xCC222222);
        graphics.fill(dx + 1, y + 1, dx + DONE_BTN_W - 1, y + PAD_BTN - 1, doneHot ? 0x664488CC : 0x44111111);
        graphics.drawCenteredString(this.font,
                Component.translatable("item.iska_utils.swiss_wrench.radial.pad.done"),
                dx + DONE_BTN_W / 2, y + (PAD_BTN - 8) / 2, doneHot ? 0xFFFFFF : 0xDDDDDD);

        if (hoveredPadMode != null) {
            graphics.renderTooltip(this.font, padTooltip(hoveredPadMode), mouseX, mouseY);
        } else if (hoveredDone) {
            graphics.renderTooltip(this.font,
                    Component.translatable("item.iska_utils.swiss_wrench.radial.pad.done_tooltip"),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

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
                graphics.drawCenteredString(this.font, label, ix, iy + SECTOR_HALF + 2, hot ? 0xFFFFFF : 0xCCCCCC);
            }

            graphics.drawCenteredString(this.font, currentStep().title(), cx, cy - 10, 0xFFFFFF);
            if (steps.size() > 1) {
                graphics.drawCenteredString(this.font,
                        Component.translatable("item.iska_utils.swiss_wrench.radial.step_progress",
                                stepIndex + 1, steps.size()),
                        cx, cy + 4, 0xAAAAAA);
            }
        }

        renderQuickPad(graphics, mouseX, mouseY);
    }

    private void renderBlockStatePreview(GuiGraphics graphics, BlockState state, int centerX, int centerY) {
        Minecraft mc = Minecraft.getInstance();
        float yaw = mc.player != null ? mc.player.getYRot() : 0.0f;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 150);
        pose.scale(PREVIEW_SCALE, -PREVIEW_SCALE, PREVIEW_SCALE);
        pose.mulPose(Axis.XP.rotationDegrees(30.0f));
        // Face toward camera matches the face the player sees looking at the block in-world.
        pose.mulPose(Axis.YP.rotationDegrees(PREVIEW_YAW_OFFSET - yaw));
        pose.translate(-0.5f, -0.5f, -0.5f);

        Lighting.setupForFlatItems();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        try {
            mc.getBlockRenderer().renderSingleBlock(
                    state, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        } catch (Exception ignored) {
            // Some modded states may fail to render in GUI; labels still show the value.
        }
        buffers.endBatch();
        Lighting.setupFor3DItems();
        RenderSystem.enableDepthTest();
        pose.popPose();
    }

    /**
     * Angle of option {@code index} around the hub (layout only; not used for hover selection).
     * Four choices sit on a cross (N/E/S/W); otherwise equal spacing starting at top.
     */
    private static double sectorAngle(int index, int count) {
        if (count == 4) {
            return -Math.PI / 2.0 + index * (Math.PI / 2.0);
        }
        double slice = (Math.PI * 2.0) / count;
        return -Math.PI / 2.0 + slice * index + slice / 2.0;
    }

    /** Hit-test option icons by their drawn square, not by pie-slice angle. */
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            return goBack();
        }
        if (button == 0) {
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
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Movement strafe keys (adaptive) + fixed arrow keys (may be the same binding).
        RotationMode quick = null;
        if (minecraft != null) {
            if (minecraft.options.keyLeft.matches(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_LEFT) {
                quick = RotationMode.ROTATE_LEFT;
            } else if (minecraft.options.keyRight.matches(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_RIGHT) {
                quick = RotationMode.ROTATE_RIGHT;
            }
        } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
            quick = RotationMode.ROTATE_LEFT;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            quick = RotationMode.ROTATE_RIGHT;
        }
        if (quick != null) {
            sendQuickRotate(quick);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (stepIndex > 0) {
                goBack();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
            return goBack();
        }
        if (keyCode == 257 || keyCode == 335) {
            confirmCurrentStep();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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

        PacketDistributor.sendToServer(new SwissWrenchRadialSubmitC2SPacket(
                pos,
                List.copyOf(chosenPropertyNames),
                List.copyOf(chosenValueNames)));
        onClose();
    }
}
