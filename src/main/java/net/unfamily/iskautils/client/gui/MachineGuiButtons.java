package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.block.entity.MobReaperBlockEntity;
import net.unfamily.iskautils.util.MachineTargetType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Shared vanilla {@link Button} / {@link ItemIconButton} helpers for machine GUIs.
 */
public final class MachineGuiButtons {
    private MachineGuiButtons() {}

    public static final int ICON_SIZE = 16;
    public static final int DOT_SIZE = 8;
    public static final String DOT_FILLED = "\u25CF";

    public static final ResourceLocation REDSTONE_GUI =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/redstone_gui.png");

    public static int displayRedstoneMode(int mode, boolean allowPulse) {
        if (mode == 3 && !allowPulse) {
            return 4;
        }
        return mode;
    }

    public static ItemStack redstoneIcon(int mode, boolean allowPulse) {
        return switch (displayRedstoneMode(mode, allowPulse)) {
            case 0 -> new ItemStack(Items.GUNPOWDER);
            case 1 -> new ItemStack(Items.REDSTONE);
            case 2 -> ItemStack.EMPTY;
            case 3 -> new ItemStack(Items.REPEATER);
            case 4 -> new ItemStack(Items.BARRIER);
            default -> new ItemStack(Items.REDSTONE);
        };
    }

    @Nullable
    public static ResourceLocation redstoneOverlay(int mode, boolean allowPulse) {
        return displayRedstoneMode(mode, allowPulse) == 2 ? REDSTONE_GUI : null;
    }

    public static Component redstoneTooltip(int mode, boolean allowPulse) {
        return switch (displayRedstoneMode(mode, allowPulse)) {
            case 0 -> Component.translatable("gui.iska_utils.generic.redstone_mode.none");
            case 1 -> Component.translatable("gui.iska_utils.generic.redstone_mode.low");
            case 2 -> Component.translatable("gui.iska_utils.generic.redstone_mode.high");
            case 3 -> Component.translatable("gui.iska_utils.generic.redstone_mode.pulse");
            case 4 -> Component.translatable("gui.iska_utils.generic.redstone_mode.disabled");
            default -> Component.literal("Unknown mode");
        };
    }

    public static ItemIconButton redstoneIconButton(
            int x, int y, Button.OnPress onPress, Supplier<Integer> mode, boolean allowPulse) {
        return new ItemIconButton(
                x,
                y,
                ICON_SIZE,
                onPress,
                () -> redstoneIcon(mode.get(), allowPulse),
                () -> redstoneOverlay(mode.get(), allowPulse),
                Component.empty());
    }

    public static ItemStack targetTypeIcon(int targetTypeId) {
        return switch (MachineTargetType.fromId(targetTypeId)) {
            case MOBS_ONLY -> new ItemStack(Items.CREEPER_HEAD);
            case MOBS_AND_PLAYERS -> new ItemStack(Items.TNT);
            case PLAYERS_ONLY -> new ItemStack(Items.PLAYER_HEAD);
        };
    }

    public static ItemStack fanPushPullIcon(boolean pull) {
        return new ItemStack(pull ? Items.STICKY_PISTON : Items.PISTON);
    }

    public static ItemStack mobAgeFilterIcon(int ageFilterId) {
        return switch (MobReaperBlockEntity.MobAgeFilter.fromId(ageFilterId)) {
            case BOTH -> new ItemStack(Items.ROTTEN_FLESH);
            case ADULTS -> new ItemStack(Items.LEATHER);
            case BABIES -> new ItemStack(Items.EGG);
        };
    }

    public static ItemStack persistentModeIcon(boolean persistent) {
        return new ItemStack(persistent ? Items.LODESTONE : Items.BRUSH);
    }

    public static Button selectionDot(int x, int y, boolean selected, Button.OnPress onPress) {
        return Button.builder(selectionDotLabel(selected), onPress)
                .bounds(x, y, DOT_SIZE, DOT_SIZE)
                .build();
    }

    public static void updateSelectionDot(Button button, boolean selected) {
        button.setMessage(selectionDotLabel(selected));
    }

    private static Component selectionDotLabel(boolean selected) {
        return selected ? Component.literal(DOT_FILLED) : Component.empty();
    }

    /** Dot X to the right of an 18px structure icon slot in a wide entry row. */
    public static int structureSelectionDotX(int entryX, int entryWidth) {
        int slotSize = 18;
        int slotX = entryX + entryWidth - slotSize - DOT_SIZE - 6;
        return slotX + slotSize + 2;
    }

    public static int structureSelectionDotY(int entryY, int entryHeight) {
        return entryY + (entryHeight - DOT_SIZE) / 2;
    }

    /** Dot X at the right edge of a filter list entry (no icon slot). */
    public static int filterSelectionDotX(int entryX, int entryWidth) {
        return entryX + entryWidth - DOT_SIZE - 4;
    }
}
