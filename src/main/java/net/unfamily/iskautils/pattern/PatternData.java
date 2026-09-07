package net.unfamily.iskautils.pattern;

import net.minecraft.nbt.CompoundTag;

/**
 * Represents a single crafting pattern - a 3x3 grid where each cell
 * can be empty (0) or assigned a letter value 1..26 (A-Z only).
 * Each pattern has its own crafting mode: 0=both, 1=shaped only, 2=shapeless only.
 */
public class PatternData {
    public static final int GRID_SIZE = 9; // 3x3
    public static final int EMPTY = 0;
    public static final int MIN_LETTER = 1;  // A
    /** Letters only A-Z (26). No limit by slot count. */
    public static final int MAX_LETTER = 26;

    /** Crafting mode: 0 = Shaped + Shapeless, 1 = Only Shaped, 2 = Only Shapeless */
    public static final int CRAFTING_MODE_BOTH = 0;
    public static final int CRAFTING_MODE_SHAPED_ONLY = 1;
    public static final int CRAFTING_MODE_SHAPELESS_ONLY = 2;

    private final int[] grid = new int[GRID_SIZE];
    /** Default: only shaped (1); user can change to both (0) or shapeless only (2) */
    private int craftingMode = CRAFTING_MODE_SHAPED_ONLY;

    /** Result mode (per pattern): 1 = Eject, 2 = Keep (always input-first), 3 = Smart (input-first only if active variable matches) */
    private int resultMode = 1;
    /** Ingredient mode (per pattern): 1 = Keep, 2 = Eject */
    private int ingredientMode = 1;
    /** Prevents tools at their breaking point from being consumed. */
    private boolean toolSafeguard = true;

    public PatternData() {
        // All cells empty by default
    }

    public int getCraftingMode() {
        return craftingMode;
    }

    public void setCraftingMode(int mode) {
        this.craftingMode = Math.max(0, Math.min(2, mode));
    }

    /** Cycles mode: both -> shaped only -> shapeless only -> both */
    public void cycleCraftingMode() {
        craftingMode = (craftingMode + 1) % 3;
    }

    public int getResultMode() {
        return resultMode;
    }

    public void setResultMode(int mode) {
        this.resultMode = Math.max(1, Math.min(3, mode));
    }

    public void cycleResultMode() {
        resultMode = resultMode >= 3 ? 1 : resultMode + 1;
    }

    public int getIngredientMode() {
        return ingredientMode;
    }

    public void setIngredientMode(int mode) {
        this.ingredientMode = Math.max(1, Math.min(2, mode));
    }

    public void cycleIngredientMode() {
        ingredientMode = ingredientMode == 1 ? 2 : 1;
    }

    public boolean isToolSafeguard() {
        return toolSafeguard;
    }

    public void setToolSafeguard(boolean toolSafeguard) {
        this.toolSafeguard = toolSafeguard;
    }

    public void toggleToolSafeguard() {
        toolSafeguard = !toolSafeguard;
    }

    public int getCell(int index) {
        if (index < 0 || index >= GRID_SIZE) return EMPTY;
        return grid[index];
    }

    public void setCell(int index, int value) {
        if (index < 0 || index >= GRID_SIZE) return;
        if (value < EMPTY || value > MAX_LETTER) value = EMPTY;
        grid[index] = value;
    }

    /**
     * Cycles the cell value: empty -> 1 -> ... -> 26 (Z) -> empty. A-Z only.
     */
    public int cycleCell(int index, int maxLetter) {
        int current = getCell(index);
        int next = current >= MAX_LETTER ? EMPTY : current + 1;
        setCell(index, next);
        return next;
    }

    /**
     * Returns the letter character for a cell value (1-26 = A-Z), or '\0' for empty or out of range.
     */
    public static char valueToChar(int value) {
        if (value < MIN_LETTER || value > MAX_LETTER) return '\0';
        return (char) ('A' + value - 1);
    }

    /** Display string: 1-26 = A-Z only. */
    public static String letterValueToDisplayString(int value) {
        if (value < MIN_LETTER || value > MAX_LETTER) return "";
        return String.valueOf((char) ('A' + value - 1));
    }

    /**
     * Returns the cell value for a letter character (A-Z = 1-26).
     */
    public static int charToValue(char c) {
        if (c < 'A' || c > 'Z') return EMPTY;
        return c - 'A' + 1;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("grid", grid.clone());
        tag.putInt("craftingMode", craftingMode);
        tag.putInt("resultMode", resultMode);
        tag.putInt("ingredientMode", ingredientMode);
        tag.putBoolean("toolSafeguard", toolSafeguard);
        return tag;
    }

    public static PatternData load(CompoundTag tag) {
        PatternData data = new PatternData();
        if (tag.contains("grid")) {
            int[] saved = tag.getIntArray("grid");
            System.arraycopy(saved, 0, data.grid, 0, Math.min(saved.length, GRID_SIZE));
        }
        if (tag.contains("craftingMode")) {
            data.craftingMode = Math.max(0, Math.min(2, tag.getInt("craftingMode")));
        }
        if (tag.contains("resultMode")) {
            data.resultMode = Math.max(1, Math.min(3, tag.getInt("resultMode")));
        }
        if (tag.contains("ingredientMode")) {
            data.ingredientMode = Math.max(1, Math.min(2, tag.getInt("ingredientMode")));
        }
        // Missing in legacy saves: preserve the safe default.
        if (tag.contains("toolSafeguard")) {
            data.toolSafeguard = tag.getBoolean("toolSafeguard");
        }
        return data;
    }
}
