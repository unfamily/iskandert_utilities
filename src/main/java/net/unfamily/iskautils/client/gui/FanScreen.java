package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.network.ModMessages;
import net.unfamily.iskautils.network.packet.FanRangeUpdateC2SPacket;

public class FanScreen extends AbstractContainerScreen<FanMenu> {
    
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            IskaUtils.MOD_ID, "textures/gui/backgrounds/fan.png");
    
    // GUI dimensions (based on fan.png: 176x200)
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 200;
    
    // Ghost items for module slots (to show what can be placed)
    private static final ItemStack GHOST_RANGE_MODULE = new ItemStack(ModItems.RANGE_MODULE.get());
    private static final ItemStack GHOST_GHOST_MODULE = new ItemStack(ModItems.GHOST_MODULE.get());
    
    // Speed modules (will cycle through these for the third slot)
    private static final ItemStack[] SPEED_MODULES = {
        new ItemStack(ModItems.SLOW_MODULE.get()),
        new ItemStack(ModItems.MODERATE_MODULE.get()),
        new ItemStack(ModItems.FAST_MODULE.get()),
        new ItemStack(ModItems.EXTREME_MODULE.get()),
        new ItemStack(ModItems.ULTRA_MODULE.get())
    };
    
    private int speedModuleCycleIndex = 0;
    private long lastCycleTime = 0;
    private static final long CYCLE_INTERVAL = 1000; // Cycle every 1 second
    
    // Range grid visualization (centered)
    private static final int GRID_SIZE = 5; // 5x5 grid
    private static final int SQUARE_SIZE = 10; // Size of each square in the grid
    private static final int GRID_TOTAL_SIZE = GRID_SIZE * SQUARE_SIZE; // Total grid size (50)
    private static final int GRID_START_X = (GUI_WIDTH - (GRID_SIZE * SQUARE_SIZE)) / 2; // Center grid horizontally
    private static final int GRID_START_Y = 30; // Position below module slots and title (moved higher)
    private static final int BUTTON_SIZE = 12; // Small buttons (like deep drawer extractor)
    private static final int BUTTON_SPACING = 1; // Space between grid and buttons (reduced to bring buttons closer)
    private static final int BAR_HEIGHT = 8; // Height of the bar below grid
    private static final int BAR_SPACING = 2; // Small space between grid and bar (bar directly below grid)
    private static final int BAR_SQUARE_COUNT = 5; // Number of squares in the bar
    private static final int CELL_BORDER = 1; // Border width for each cell to make them visibly separated
    private static final int CELL_BORDER_COLOR = 0xFFC6C6C6; // Light gray border color (#c6c6c6)
    private static final int GRID_BUTTON_OFFSET = 8; // Offset from grid center to bring buttons closer together
    
    // Right side buttons (redstone mode, push/pull, push type)
    private static final ResourceLocation MEDIUM_BUTTONS = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/medium_buttons.png");
    private static final ResourceLocation REDSTONE_GUI = ResourceLocation.fromNamespaceAndPath("iska_utils", "textures/gui/redstone_gui.png");
    private static final int REDSTONE_BUTTON_SIZE = 16;
    private static final int BUTTON_SPACING_Y = 4; // Vertical spacing between buttons on right side
    private static final int RIGHT_BUTTON_MARGIN = 10; // Margin from right edge
    
    // Close button (X)
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;
    
    public FanScreen(FanMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }
    
    // Store button references
    private Button topLeftButton, topRightButton; // Above grid
    private Button leftTopButton, leftBottomButton; // Left of grid
    private Button rightTopButton, rightBottomButton; // Right of grid
    private Button bottomLeftButton, bottomRightButton; // Below grid
    private Button barLeftButton, barRightButton; // In the bar
    private Button closeButton; // X button top right
    private Button showButton; // Show button
    private int redstoneModeButtonX, redstoneModeButtonY; // Redstone mode button position
    private int pushPullButtonX, pushPullButtonY; // Push/Pull button position
    private int pushTypeButtonX, pushTypeButtonY; // Push type button position
    
    @Override
    protected void init() {
        super.init();
        
        // Center the GUI on screen
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
        // Create close button (X) - top right
        closeButton = Button.builder(Component.literal("✕"), 
                                    button -> {
                                        if (this.minecraft != null) {
                                            this.minecraft.player.closeContainer();
                                        }
                                    })
                           .bounds(this.leftPos + CLOSE_BUTTON_X, this.topPos + CLOSE_BUTTON_Y, 
                                  CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                           .build();
        this.addRenderableWidget(closeButton);
        
        // Calculate right side button positions (centered vertically with grid)
        int rightButtonX = this.leftPos + this.imageWidth - RIGHT_BUTTON_MARGIN - REDSTONE_BUTTON_SIZE;
        int gridCenterY = this.topPos + GRID_START_Y + GRID_TOTAL_SIZE / 2;
        redstoneModeButtonY = gridCenterY - REDSTONE_BUTTON_SIZE - BUTTON_SPACING_Y - REDSTONE_BUTTON_SIZE / 2;
        pushPullButtonY = gridCenterY - REDSTONE_BUTTON_SIZE / 2;
        pushTypeButtonY = gridCenterY + BUTTON_SPACING_Y + REDSTONE_BUTTON_SIZE / 2;
        redstoneModeButtonX = rightButtonX;
        pushPullButtonX = rightButtonX;
        pushTypeButtonX = rightButtonX;
        
        // Create range adjustment buttons around the grid
        createRangeButtons();
        
        // Create "Show" button - aligned with right side buttons (redstoneMode, etc.)
        int gridY = this.topPos + GRID_START_Y;
        int bottomButtonY = gridY + GRID_TOTAL_SIZE + BUTTON_SPACING;
        int labelY = bottomButtonY + BUTTON_SIZE + 2;
        
        Component showText = Component.translatable("gui.iska_utils.generic.show");
        int buttonWidth = this.font.width(showText) + 6; // Minimal width: text + 3px padding each side
        int buttonHeight = this.font.lineHeight + 2; // Minimal height: text + 1px padding each side
        
        // Align right edge with right edge of redstoneMode buttons above
        int rightButtonsRightEdge = rightButtonX + REDSTONE_BUTTON_SIZE;
        int showButtonX = rightButtonsRightEdge - buttonWidth; // Align right edge
        int showButtonY = labelY + (this.font.lineHeight - buttonHeight) / 2; // Vertically centered with label text
        
        showButton = Button.builder(
            showText,
            button -> onShowPressed()
        ).bounds(showButtonX, showButtonY, buttonWidth, buttonHeight).build();
        
        this.addRenderableWidget(showButton);
    }
    
    private void createRangeButtons() {
        int gridX = this.leftPos + GRID_START_X;
        int gridY = this.topPos + GRID_START_Y;
        int gridCenterX = gridX + GRID_TOTAL_SIZE / 2;
        int gridCenterY = gridY + GRID_TOTAL_SIZE / 2;
        
        // Buttons above grid: - left, + right (closer together, positioned near center)
        int topButtonY = gridY - BUTTON_SIZE - BUTTON_SPACING;
        topLeftButton = createButton("-", gridCenterX - GRID_BUTTON_OFFSET - BUTTON_SIZE, 
            topButtonY, FanRangeUpdateC2SPacket.RangeType.UP, -1);
        topRightButton = createButton("+", gridCenterX + GRID_BUTTON_OFFSET, 
            topButtonY, FanRangeUpdateC2SPacket.RangeType.UP, 1);
        
        // Buttons left of grid: + top, - bottom (use same spacing as top buttons horizontally)
        // Top buttons spacing: GRID_BUTTON_OFFSET (8 pixels) between - and + buttons
        leftTopButton = createButton("+", gridX - BUTTON_SIZE - BUTTON_SPACING, 
            gridCenterY - GRID_BUTTON_OFFSET - BUTTON_SIZE, FanRangeUpdateC2SPacket.RangeType.LEFT, 1);
        leftBottomButton = createButton("-", gridX - BUTTON_SIZE - BUTTON_SPACING, 
            gridCenterY + GRID_BUTTON_OFFSET, FanRangeUpdateC2SPacket.RangeType.LEFT, -1);
        
        // Buttons right of grid: + top, - bottom (use same spacing as top buttons horizontally)
        rightTopButton = createButton("+", gridX + GRID_TOTAL_SIZE + BUTTON_SPACING, 
            gridCenterY - GRID_BUTTON_OFFSET - BUTTON_SIZE, FanRangeUpdateC2SPacket.RangeType.RIGHT, 1);
        rightBottomButton = createButton("-", gridX + GRID_TOTAL_SIZE + BUTTON_SPACING, 
            gridCenterY + GRID_BUTTON_OFFSET, FanRangeUpdateC2SPacket.RangeType.RIGHT, -1);
        
        // Buttons below grid: - left, + right (closer together, positioned near center, directly below grid)
        int bottomButtonY = gridY + GRID_TOTAL_SIZE + BUTTON_SPACING;
        bottomLeftButton = createButton("-", gridCenterX - GRID_BUTTON_OFFSET - BUTTON_SIZE, 
            bottomButtonY, FanRangeUpdateC2SPacket.RangeType.DOWN, -1);
        bottomRightButton = createButton("+", gridCenterX + GRID_BUTTON_OFFSET, 
            bottomButtonY, FanRangeUpdateC2SPacket.RangeType.DOWN, 1);
        
        // Bar position (below the bottom buttons, after label) - centered
        // Label height is typically 9 pixels, spacing is 2 pixels
        int barY = bottomButtonY + BUTTON_SIZE + 9 + 2; // After buttons + label height + spacing
        int barTotalWidth = BAR_SQUARE_COUNT * SQUARE_SIZE;
        int totalBarWidth = BUTTON_SIZE + barTotalWidth + BUTTON_SIZE; // Left button + squares + right button
        int barX = this.leftPos + (GUI_WIDTH - totalBarWidth) / 2; // Center the entire bar (buttons + squares) relative to GUI
        
        // Buttons in the bar: - at start, + at end (buttons on sides, squares in between)
        barLeftButton = createButton("-", barX, barY, FanRangeUpdateC2SPacket.RangeType.FORWARD, -1);
        // Squares start after the left button
        int squaresStartX = barX + BUTTON_SIZE;
        barRightButton = createButton("+", squaresStartX + barTotalWidth, barY, 
            FanRangeUpdateC2SPacket.RangeType.FORWARD, 1);
    }
    
    private Button createButton(String label, int x, int y, FanRangeUpdateC2SPacket.RangeType rangeType, int delta) {
        Button button = Button.builder(Component.literal(label), 
            btn -> {
                playButtonSound();
                adjustRange(rangeType, delta);
            })
            .bounds(x, y, BUTTON_SIZE, BUTTON_SIZE)
            .build();
        addRenderableWidget(button);
        return button;
    }
    
    private void adjustRange(FanRangeUpdateC2SPacket.RangeType rangeType, int delta) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(BlockPos.ZERO)) {
            ModMessages.sendFanRangeUpdatePacket(pos, rangeType, delta);
        }
    }
    
    private void playButtonSound() {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
        }
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int guiX = this.leftPos;
        int guiY = this.topPos;
        
        // Render main background
        guiGraphics.blit(BACKGROUND, guiX, guiY, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
        
        // Render range grid visualization
        renderRangeGrid(guiGraphics);
        
        // Render right side buttons
        renderRedstoneModeButton(guiGraphics, mouseX, mouseY);
        renderPushPullButton(guiGraphics, mouseX, mouseY);
        renderPushTypeButton(guiGraphics, mouseX, mouseY);
    }
    
    private void renderRangeGrid(GuiGraphics guiGraphics) {
        int gridX = this.leftPos + GRID_START_X;
        int gridY = this.topPos + GRID_START_Y;
        
        // Get current range values
        int rangeUp = menu.getRangeUp();
        int rangeDown = menu.getRangeDown();
        int rangeLeft = menu.getRangeLeft();
        int rangeRight = menu.getRangeRight();
        int rangeFront = menu.getRangeFront();
        
        // Get max values from config
        int maxVertical = Config.fanRangeVerticalMax;
        int maxHorizontal = Config.fanRangeHorizontalMax;
        int maxFront = Config.fanRangeFrontMax;
        
        // Render 5x5 grid
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int squareX = gridX + col * SQUARE_SIZE;
                int squareY = gridY + row * SQUARE_SIZE;
                
                // Calculate distance from center (row 2, col 2)
                int centerRow = 2;
                int centerCol = 2;
                int distRow = Math.abs(row - centerRow);
                int distCol = Math.abs(col - centerCol);
                
                // Center square (fan position)
                if (row == centerRow && col == centerCol) {
                    // Render fan position: black if rangeFront is 0, darker green otherwise
                    int centerColor = (rangeFront == 0) ? 0xFF000000 : 0xFF008800;
                    guiGraphics.fill(squareX, squareY, squareX + SQUARE_SIZE, squareY + SQUARE_SIZE, centerColor);
                    // Draw thin border around center cell (lighter gray, thinner)
                    guiGraphics.hLine(squareX, squareX + SQUARE_SIZE - 1, squareY, CELL_BORDER_COLOR); // Top
                    guiGraphics.hLine(squareX, squareX + SQUARE_SIZE - 1, squareY + SQUARE_SIZE - 1, CELL_BORDER_COLOR); // Bottom
                    guiGraphics.vLine(squareX, squareY, squareY + SQUARE_SIZE - 1, CELL_BORDER_COLOR); // Left
                    guiGraphics.vLine(squareX + SQUARE_SIZE - 1, squareY, squareY + SQUARE_SIZE - 1, CELL_BORDER_COLOR); // Right
                    continue;
                }
                
                // Determine which ranges apply to this square
                // Each range applies independently, and perpendicular ranges create "derived" highlights
                
                // For up: rows above center, same column, and within range distance
                boolean inUpRange = (col == centerCol) && (row < centerRow) && (distRow <= rangeUp) && (rangeUp > 0);
                // For down: rows below center, same column, and within range distance
                boolean inDownRange = (col == centerCol) && (row > centerRow) && (distRow <= rangeDown) && (rangeDown > 0);
                // For left: columns left of center, same row, and within range distance
                boolean inLeftRange = (row == centerRow) && (col < centerCol) && (distCol <= rangeLeft) && (rangeLeft > 0);
                // For right: columns right of center, same row, and within range distance
                boolean inRightRange = (row == centerRow) && (col > centerCol) && (distCol <= rangeRight) && (rangeRight > 0);
                
                // Derived highlights: when perpendicular ranges are active, highlight squares in the combined direction
                // Example: UP=1 and RIGHT=1 -> highlight squares above in the right column
                if (rangeUp > 0 && rangeRight > 0) {
                    // Squares above and to the right (derived from UP + RIGHT)
                    if (row < centerRow && col > centerCol && distRow <= rangeUp && distCol <= rangeRight) {
                        inUpRange = true;
                        inRightRange = true;
                    }
                }
                if (rangeUp > 0 && rangeLeft > 0) {
                    // Squares above and to the left (derived from UP + LEFT)
                    if (row < centerRow && col < centerCol && distRow <= rangeUp && distCol <= rangeLeft) {
                        inUpRange = true;
                        inLeftRange = true;
                    }
                }
                if (rangeDown > 0 && rangeRight > 0) {
                    // Squares below and to the right (derived from DOWN + RIGHT)
                    if (row > centerRow && col > centerCol && distRow <= rangeDown && distCol <= rangeRight) {
                        inDownRange = true;
                        inRightRange = true;
                    }
                }
                if (rangeDown > 0 && rangeLeft > 0) {
                    // Squares below and to the left (derived from DOWN + LEFT)
                    if (row > centerRow && col < centerCol && distRow <= rangeDown && distCol <= rangeLeft) {
                        inDownRange = true;
                        inLeftRange = true;
                    }
                }
                
                // Count how many ranges apply (for hybrid corners)
                int rangeCount = 0;
                if (inUpRange) rangeCount++;
                if (inDownRange) rangeCount++;
                if (inLeftRange) rangeCount++;
                if (inRightRange) rangeCount++;
                
                // Determine color based on range state
                int color;
                
                if (rangeCount == 0) {
                    // No range active - gray
                    color = 0xFF666666;
                } else if (rangeCount > 1) {
                    // Hybrid corner or derived square: check ranges involved
                    int representableMax = 2; // Maximum distance representable in 5x5 grid (center at 2,2)
                    boolean exceedsRepresentableUp = inUpRange && rangeUp > representableMax;
                    boolean exceedsRepresentableDown = inDownRange && rangeDown > representableMax;
                    boolean exceedsRepresentableLeft = inLeftRange && rangeLeft > representableMax;
                    boolean exceedsRepresentableRight = inRightRange && rangeRight > representableMax;
                    boolean exceedsConfigMaxUp = inUpRange && rangeUp > maxVertical;
                    boolean exceedsConfigMaxDown = inDownRange && rangeDown > maxVertical;
                    boolean exceedsConfigMaxLeft = inLeftRange && rangeLeft > maxHorizontal;
                    boolean exceedsConfigMaxRight = inRightRange && rangeRight > maxHorizontal;
                    
                    // Count how many ranges exceed representable (2)
                    int exceedsRepresentableCount = 0;
                    if (exceedsRepresentableUp) exceedsRepresentableCount++;
                    if (exceedsRepresentableDown) exceedsRepresentableCount++;
                    if (exceedsRepresentableLeft) exceedsRepresentableCount++;
                    if (exceedsRepresentableRight) exceedsRepresentableCount++;
                    
                    // Check if square is within representable distance
                    boolean squareWithinRepresentable = true;
                    if (inUpRange && distRow > representableMax) squareWithinRepresentable = false;
                    if (inDownRange && distRow > representableMax) squareWithinRepresentable = false;
                    if (inLeftRange && distCol > representableMax) squareWithinRepresentable = false;
                    if (inRightRange && distCol > representableMax) squareWithinRepresentable = false;
                    
                    // Determine color: aqua if only one range exceeds representable and square is within representable
                    if (exceedsRepresentableCount == 1 && squareWithinRepresentable) {
                        // Only one range exceeds representable, square is within representable -> aqua
                        color = 0xFF00FFFF; // Aqua
                    } else if (exceedsRepresentableCount > 0 || exceedsConfigMaxUp || exceedsConfigMaxDown || 
                               exceedsConfigMaxLeft || exceedsConfigMaxRight) {
                        // At least one range exceeds limits -> blue
                        color = 0xFF0000FF; // Blue
                    } else {
                        // All ranges within limits -> green
                        color = 0xFF00FF00; // Green
                    }
                } else {
                    // Single range - determine color based on range value
                    int representableMax = 2; // Maximum distance representable in 5x5 grid (center at 2,2)
                    
                    if (inUpRange) {
                        // If range exceeds representable distance, all involved squares should be blue
                        if (rangeUp > representableMax) {
                            color = 0xFF0000FF; // Blue (exceeds representable)
                        } else if (rangeUp > maxVertical) {
                            color = 0xFF0000FF; // Blue (exceeds config max)
                        } else {
                            color = 0xFF00FF00; // Green (active, within limits)
                        }
                    } else if (inDownRange) {
                        // If range exceeds representable distance, all involved squares should be blue
                        if (rangeDown > representableMax) {
                            color = 0xFF0000FF; // Blue (exceeds representable)
                        } else if (rangeDown > maxVertical) {
                            color = 0xFF0000FF; // Blue (exceeds config max)
                        } else {
                            color = 0xFF00FF00; // Green (active, within limits)
                        }
                    } else if (inLeftRange) {
                        // If range exceeds representable distance, all involved squares should be blue
                        if (rangeLeft > representableMax) {
                            color = 0xFF0000FF; // Blue (exceeds representable)
                        } else if (rangeLeft > maxHorizontal) {
                            color = 0xFF0000FF; // Blue (exceeds config max)
                        } else {
                            color = 0xFF00FF00; // Green (active, within limits)
                        }
                    } else if (inRightRange) {
                        // If range exceeds representable distance, all involved squares should be blue
                        if (rangeRight > representableMax) {
                            color = 0xFF0000FF; // Blue (exceeds representable)
                        } else if (rangeRight > maxHorizontal) {
                            color = 0xFF0000FF; // Blue (exceeds config max)
                        } else {
                            color = 0xFF00FF00; // Green (active, within limits)
                        }
                    } else {
                        color = 0xFF666666; // Gray (not active)
                    }
                }
                
                // Render square with thin border to make cells visibly separated
                guiGraphics.fill(squareX, squareY, squareX + SQUARE_SIZE, squareY + SQUARE_SIZE, color);
                // Draw thin borders (light gray lines) to separate cells
                // Always draw borders, including outer edges of the grid
                guiGraphics.hLine(squareX, squareX + SQUARE_SIZE - 1, squareY, CELL_BORDER_COLOR); // Top border
                guiGraphics.hLine(squareX, squareX + SQUARE_SIZE - 1, squareY + SQUARE_SIZE - 1, CELL_BORDER_COLOR); // Bottom border
                guiGraphics.vLine(squareX, squareY, squareY + SQUARE_SIZE - 1, CELL_BORDER_COLOR); // Left border
                guiGraphics.vLine(squareX + SQUARE_SIZE - 1, squareY, squareY + SQUARE_SIZE - 1, CELL_BORDER_COLOR); // Right border
            }
        }
        
        // Calculate positions: buttons below grid, then label, then bar
        int bottomButtonY = gridY + GRID_TOTAL_SIZE + BUTTON_SPACING;
        int labelY = bottomButtonY + BUTTON_SIZE + 2; // After bottom buttons
        int barY = labelY + this.font.lineHeight + 2; // After label
        int barTotalWidth = BAR_SQUARE_COUNT * SQUARE_SIZE;
        int totalBarWidth = BUTTON_SIZE + barTotalWidth + BUTTON_SIZE; // Left button + squares + right button
        int barX = this.leftPos + (GUI_WIDTH - totalBarWidth) / 2; // Center the entire bar (buttons + squares) relative to GUI
        int squaresStartX = barX + BUTTON_SIZE; // Squares start after left button
        
        // Render "forward" label (translatable, between bottom buttons and bar, centered)
        Component forwardLabel = Component.translatable("gui.iska_utils.fan.forward");
        int labelX = this.leftPos + (GUI_WIDTH - this.font.width(forwardLabel)) / 2; // Center label relative to GUI
        guiGraphics.drawString(this.font, forwardLabel, labelX, labelY, 0x404040, false);
        
        // Render bar squares with borders (between the buttons)
        for (int i = 0; i < BAR_SQUARE_COUNT; i++) {
            int squareX = squaresStartX + i * SQUARE_SIZE;
            int color;
            
            if (i < rangeFront) {
                if (rangeFront == 0) {
                    color = 0xFF666666; // Gray (not active)
                } else if (rangeFront <= BAR_SQUARE_COUNT) {
                    color = 0xFF00FF00; // Green (<= 5)
                } else {
                    color = 0xFF0000FF; // Blue (>= 6)
                }
            } else {
                color = 0xFF666666; // Gray (not in range)
            }
            
            guiGraphics.fill(squareX, barY, squareX + SQUARE_SIZE, barY + BAR_HEIGHT, color);
            // Draw thin borders to separate bar squares (light gray, thinner)
            // Left border (always draw, including first square)
            guiGraphics.vLine(squareX, barY, barY + BAR_HEIGHT - 1, CELL_BORDER_COLOR);
            // Right border (always draw, including last square)
            guiGraphics.vLine(squareX + SQUARE_SIZE - 1, barY, barY + BAR_HEIGHT - 1, CELL_BORDER_COLOR);
            // Top border
            guiGraphics.hLine(squareX, squareX + SQUARE_SIZE - 1, barY, CELL_BORDER_COLOR);
            // Bottom border
            guiGraphics.hLine(squareX, squareX + SQUARE_SIZE - 1, barY + BAR_HEIGHT - 1, CELL_BORDER_COLOR);
        }
    }
    
    /**
     * Renders a single ghost item (semi-transparent) at the specified position
     */
    private void renderGhostItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        // Save current matrix state
        guiGraphics.pose().pushPose();
        
        // Translate to the slot position (relative to GUI)
        guiGraphics.pose().translate(this.leftPos + x, this.topPos + y, 0);
        
        // Render the item first
        guiGraphics.renderItem(itemStack, 0, 0);
        
        // Then apply a semi-transparent dark overlay to create ghost effect
        guiGraphics.fill(0, 0, 16, 16, 0x80000000); // 50% transparent black overlay
        
        // Restore matrix state
        guiGraphics.pose().popPose();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render the background (includes grid)
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Render ghost items on top of empty module slots
        renderGhostItems(guiGraphics);
        
        // Render tooltips
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        // Render tooltips for right side buttons
        renderButtonTooltips(guiGraphics, mouseX, mouseY);
    }
    
    private void renderButtonTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Redstone mode button tooltip
        if (mouseX >= redstoneModeButtonX && mouseX <= redstoneModeButtonX + REDSTONE_BUTTON_SIZE &&
            mouseY >= redstoneModeButtonY && mouseY <= redstoneModeButtonY + REDSTONE_BUTTON_SIZE) {
            // Get current redstone mode from menu
            int redstoneMode = menu.getRedstoneMode();
            
            // Draw the appropriate tooltip
            Component tooltip = switch (redstoneMode) {
                case 0 -> Component.translatable("gui.iska_utils.generic.redstone_mode.none");
                case 1 -> Component.translatable("gui.iska_utils.generic.redstone_mode.low");
                case 2 -> Component.translatable("gui.iska_utils.generic.redstone_mode.high");
                case 4 -> Component.translatable("gui.iska_utils.generic.redstone_mode.disabled");
                default -> Component.literal("Unknown mode");
            };
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
        
        // Push/Pull button tooltip
        if (mouseX >= pushPullButtonX && mouseX <= pushPullButtonX + REDSTONE_BUTTON_SIZE &&
            mouseY >= pushPullButtonY && mouseY <= pushPullButtonY + REDSTONE_BUTTON_SIZE) {
            // Show current state (Push or Pull)
            boolean isPull = menu.isPull();
            Component tooltip = Component.translatable(isPull ? "gui.iska_utils.fan.push_pull.pull" : "gui.iska_utils.fan.push_pull.push");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
        
        // Push type button tooltip
        if (mouseX >= pushTypeButtonX && mouseX <= pushTypeButtonX + REDSTONE_BUTTON_SIZE &&
            mouseY >= pushTypeButtonY && mouseY <= pushTypeButtonY + REDSTONE_BUTTON_SIZE) {
            int pushTypeId = menu.getPushType();
            net.unfamily.iskautils.block.entity.FanBlockEntity.PushType pushType = 
                net.unfamily.iskautils.block.entity.FanBlockEntity.PushType.fromId(pushTypeId);
            Component tooltip = Component.translatable("gui.iska_utils.fan.push_type." + pushType.getName());
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
        
        // Range adjustment buttons tooltips
        renderRangeButtonTooltips(guiGraphics, mouseX, mouseY);
    }
    
    private void renderRangeButtonTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Helper method to check if mouse is over a button and render tooltip
        if (topLeftButton != null && isMouseOverButton(mouseX, mouseY, topLeftButton)) {
            renderRangeButtonTooltip(guiGraphics, mouseX, mouseY, FanRangeUpdateC2SPacket.RangeType.UP, -1);
        } else if (topRightButton != null && isMouseOverButton(mouseX, mouseY, topRightButton)) {
            renderRangeButtonTooltip(guiGraphics, mouseX, mouseY, FanRangeUpdateC2SPacket.RangeType.UP, 1);
        } else if (leftTopButton != null && isMouseOverButton(mouseX, mouseY, leftTopButton)) {
            renderRangeButtonTooltip(guiGraphics, mouseX, mouseY, FanRangeUpdateC2SPacket.RangeType.LEFT, 1);
        } else if (leftBottomButton != null && isMouseOverButton(mouseX, mouseY, leftBottomButton)) {
            renderRangeButtonTooltip(guiGraphics, mouseX, mouseY, FanRangeUpdateC2SPacket.RangeType.LEFT, -1);
        } else if (rightTopButton != null && isMouseOverButton(mouseX, mouseY, rightTopButton)) {
            renderRangeButtonTooltip(guiGraphics, mouseX, mouseY, FanRangeUpdateC2SPacket.RangeType.RIGHT, 1);
        } else if (rightBottomButton != null && isMouseOverButton(mouseX, mouseY, rightBottomButton)) {
            renderRangeButtonTooltip(guiGraphics, mouseX, mouseY, FanRangeUpdateC2SPacket.RangeType.RIGHT, -1);
        } else if (bottomLeftButton != null && isMouseOverButton(mouseX, mouseY, bottomLeftButton)) {
            renderRangeButtonTooltip(guiGraphics, mouseX, mouseY, FanRangeUpdateC2SPacket.RangeType.DOWN, -1);
        } else if (bottomRightButton != null && isMouseOverButton(mouseX, mouseY, bottomRightButton)) {
            renderRangeButtonTooltip(guiGraphics, mouseX, mouseY, FanRangeUpdateC2SPacket.RangeType.DOWN, 1);
        } else if (barLeftButton != null && isMouseOverButton(mouseX, mouseY, barLeftButton)) {
            renderRangeButtonTooltip(guiGraphics, mouseX, mouseY, FanRangeUpdateC2SPacket.RangeType.FORWARD, -1);
        } else if (barRightButton != null && isMouseOverButton(mouseX, mouseY, barRightButton)) {
            renderRangeButtonTooltip(guiGraphics, mouseX, mouseY, FanRangeUpdateC2SPacket.RangeType.FORWARD, 1);
        }
    }
    
    private boolean isMouseOverButton(int mouseX, int mouseY, Button button) {
        return mouseX >= button.getX() && mouseX <= button.getX() + button.getWidth() &&
               mouseY >= button.getY() && mouseY <= button.getY() + button.getHeight();
    }
    
    private void renderRangeButtonTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, 
                                         FanRangeUpdateC2SPacket.RangeType rangeType, int delta) {
        // Get current range value
        int currentValue = switch (rangeType) {
            case UP -> menu.getRangeUp();
            case DOWN -> menu.getRangeDown();
            case LEFT -> menu.getRangeLeft();
            case RIGHT -> menu.getRangeRight();
            case FORWARD -> menu.getRangeFront();
        };
        
        // Get direction name
        String directionKey = switch (rangeType) {
            case UP -> "up";
            case DOWN -> "down";
            case LEFT -> "left";
            case RIGHT -> "right";
            case FORWARD -> "forward";
        };
        
        // Get increment/decrement text
        String actionKey = delta > 0 ? "increment" : "decrement";
        
        // Create tooltip with two separate lines: action + direction, and value
        Component line1 = Component.translatable("gui.iska_utils.fan.range_button." + actionKey, 
            Component.translatable("gui.iska_utils.fan.range_button.direction." + directionKey));
        Component line2 = Component.translatable("gui.iska_utils.fan.range_button.blocks", currentValue);
        
        // Convert Components to FormattedCharSequence for multi-line tooltip
        java.util.List<net.minecraft.util.FormattedCharSequence> tooltipLines = java.util.List.of(
            line1.getVisualOrderText(),
            line2.getVisualOrderText()
        );
        guiGraphics.renderTooltip(this.font, tooltipLines, mouseX, mouseY);
    }
    
    /**
     * Renders ghost items (semi-transparent) in module slots that are empty
     */
    private void renderGhostItems(GuiGraphics guiGraphics) {
        // Slot 1: range_module ghost
        net.minecraft.world.inventory.Slot slot0 = this.menu.getSlot(0);
        if (slot0.getItem().isEmpty()) {
            renderGhostItem(guiGraphics, GHOST_RANGE_MODULE, slot0.x, slot0.y);
        }
        
        // Slot 2: ghost_module ghost
        net.minecraft.world.inventory.Slot slot1 = this.menu.getSlot(1);
        if (slot1.getItem().isEmpty()) {
            renderGhostItem(guiGraphics, GHOST_GHOST_MODULE, slot1.x, slot1.y);
        }
        
        // Slot 3: speed module ghost (cycles through vector plates)
        net.minecraft.world.inventory.Slot slot2 = this.menu.getSlot(2);
        if (slot2.getItem().isEmpty()) {
            // Cycle through speed modules
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCycleTime >= CYCLE_INTERVAL) {
                speedModuleCycleIndex = (speedModuleCycleIndex + 1) % SPEED_MODULES.length;
                lastCycleTime = currentTime;
            }
            
            ItemStack currentSpeedModule = SPEED_MODULES[speedModuleCycleIndex];
            renderGhostItem(guiGraphics, currentSpeedModule, slot2.x, slot2.y);
        }
    }
    
    private void renderRedstoneModeButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Check if mouse is over the button
        boolean isHovered = mouseX >= redstoneModeButtonX && mouseX <= redstoneModeButtonX + REDSTONE_BUTTON_SIZE &&
                           mouseY >= redstoneModeButtonY && mouseY <= redstoneModeButtonY + REDSTONE_BUTTON_SIZE;
        
        // Draw button background (normal or highlighted)
        int textureY = isHovered ? 16 : 0;
        guiGraphics.blit(MEDIUM_BUTTONS, redstoneModeButtonX, redstoneModeButtonY, 
                        0, textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 
                        96, 96);
        
        // Get current redstone mode from menu
        int redstoneMode = menu.getRedstoneMode();
        
        // Draw the appropriate icon (12x12 pixels, centered in the 16x16 button)
        int iconX = redstoneModeButtonX + 2;
        int iconY = redstoneModeButtonY + 2;
        int iconSize = 12;
        
        switch (redstoneMode) {
            case 0 -> {
                // NONE mode: Gunpowder icon
                ItemStack gunpowder = new ItemStack(net.minecraft.world.item.Items.GUNPOWDER);
                renderScaledItem(guiGraphics, gunpowder, iconX, iconY, iconSize);
            }
            case 1 -> {
                // LOW mode: Redstone dust icon
                ItemStack redstone = new ItemStack(net.minecraft.world.item.Items.REDSTONE);
                renderScaledItem(guiGraphics, redstone, iconX, iconY, iconSize);
            }
            case 2 -> {
                // HIGH mode: Redstone GUI texture
                renderScaledTexture(guiGraphics, REDSTONE_GUI, iconX, iconY, iconSize);
            }
            case 4 -> {
                // DISABLED mode: Barrier icon
                ItemStack barrier = new ItemStack(net.minecraft.world.item.Items.BARRIER);
                renderScaledItem(guiGraphics, barrier, iconX, iconY, iconSize);
            }
        }
    }
    
    private void renderPushPullButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Check if mouse is over the button
        boolean isHovered = mouseX >= pushPullButtonX && mouseX <= pushPullButtonX + REDSTONE_BUTTON_SIZE &&
                           mouseY >= pushPullButtonY && mouseY <= pushPullButtonY + REDSTONE_BUTTON_SIZE;
        
        // Draw button background
        int textureY = isHovered ? 16 : 0;
        guiGraphics.blit(MEDIUM_BUTTONS, pushPullButtonX, pushPullButtonY, 
                        0, textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 
                        96, 96);
        
        // Get current push/pull state
        boolean isPull = menu.isPull();
        
        // Draw icon: piston for push, sticky piston for pull
        int iconX = pushPullButtonX + 2;
        int iconY = pushPullButtonY + 2;
        int iconSize = 12;
        
        if (isPull) {
            // Pull: sticky piston
            ItemStack stickyPiston = new ItemStack(net.minecraft.world.item.Items.STICKY_PISTON);
            renderScaledItem(guiGraphics, stickyPiston, iconX, iconY, iconSize);
        } else {
            // Push: regular piston
            ItemStack piston = new ItemStack(net.minecraft.world.item.Items.PISTON);
            renderScaledItem(guiGraphics, piston, iconX, iconY, iconSize);
        }
    }
    
    private void renderPushTypeButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Check if mouse is over the button
        boolean isHovered = mouseX >= pushTypeButtonX && mouseX <= pushTypeButtonX + REDSTONE_BUTTON_SIZE &&
                           mouseY >= pushTypeButtonY && mouseY <= pushTypeButtonY + REDSTONE_BUTTON_SIZE;
        
        // Draw button background
        int textureY = isHovered ? 16 : 0;
        guiGraphics.blit(MEDIUM_BUTTONS, pushTypeButtonX, pushTypeButtonY, 
                        0, textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 
                        96, 96);
        
        // Get current push type
        int pushTypeId = menu.getPushType();
        net.unfamily.iskautils.block.entity.FanBlockEntity.PushType pushType = 
            net.unfamily.iskautils.block.entity.FanBlockEntity.PushType.fromId(pushTypeId);
        
        // Draw icon based on push type
        int iconX = pushTypeButtonX + 2;
        int iconY = pushTypeButtonY + 2;
        int iconSize = 12;
        
        switch (pushType) {
            case MOBS_ONLY -> {
                // Mobs only: creeper head
                ItemStack creeperHead = new ItemStack(net.minecraft.world.item.Items.CREEPER_HEAD);
                renderScaledItem(guiGraphics, creeperHead, iconX, iconY, iconSize);
            }
            case MOBS_AND_PLAYERS -> {
                // Mobs and players: TNT
                ItemStack tnt = new ItemStack(net.minecraft.world.item.Items.TNT);
                renderScaledItem(guiGraphics, tnt, iconX, iconY, iconSize);
            }
            case PLAYERS_ONLY -> {
                // Players only: player head
                ItemStack playerHead = new ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
                renderScaledItem(guiGraphics, playerHead, iconX, iconY, iconSize);
            }
        }
    }
    
    /**
     * Renders an item scaled to the specified size
     */
    private void renderScaledItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y, int size) {
        guiGraphics.pose().pushPose();
        float scale = (float) size / 16.0f;
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.renderItem(itemStack, 0, 0);
        guiGraphics.pose().popPose();
    }
    
    /**
     * Renders a texture scaled to the specified size (like an item)
     */
    private void renderScaledTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int size) {
        guiGraphics.pose().pushPose();
        float scale = (float) size / 16.0f;
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
        guiGraphics.pose().popPose();
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            // Check redstone mode button
            if (mouseX >= redstoneModeButtonX && mouseX <= redstoneModeButtonX + REDSTONE_BUTTON_SIZE &&
                mouseY >= redstoneModeButtonY && mouseY <= redstoneModeButtonY + REDSTONE_BUTTON_SIZE) {
                onRedstoneModePressed();
                return true;
            }
            
            // Check push/pull button
            if (mouseX >= pushPullButtonX && mouseX <= pushPullButtonX + REDSTONE_BUTTON_SIZE &&
                mouseY >= pushPullButtonY && mouseY <= pushPullButtonY + REDSTONE_BUTTON_SIZE) {
                onPushPullPressed();
                return true;
            }
            
            // Check push type button
            if (mouseX >= pushTypeButtonX && mouseX <= pushTypeButtonX + REDSTONE_BUTTON_SIZE &&
                mouseY >= pushTypeButtonY && mouseY <= pushTypeButtonY + REDSTONE_BUTTON_SIZE) {
                onPushTypePressed();
                return true;
            }
            
            // Show button is handled by vanilla Button widget
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void onRedstoneModePressed() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(BlockPos.ZERO)) {
            ModMessages.sendFanRedstoneModePacket(pos);
            playButtonSound();
        }
    }
    
    private void onPushPullPressed() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(BlockPos.ZERO)) {
            ModMessages.sendFanPushPullPacket(pos);
            playButtonSound();
        }
    }
    
    private void onPushTypePressed() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(BlockPos.ZERO)) {
            ModMessages.sendFanPushTypePacket(pos);
            playButtonSound();
        }
    }
    
    private void onShowPressed() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(BlockPos.ZERO)) {
            ModMessages.sendFanShowAreaPacket(pos);
            playButtonSound();
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render title centered (title is already translatable via FanBlockEntity.getDisplayName())
        Component titleComponent = this.title;
        int titleWidth = this.font.width(titleComponent);
        int titleX = (this.imageWidth - titleWidth) / 2;
        guiGraphics.drawString(this.font, titleComponent, titleX, 8, 0x404040, false);
    }
}


