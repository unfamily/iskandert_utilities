package net.unfamily.iskautils.guide;

import net.unfamily.iskautils.util.ModLogger;

import guideme.Guide;
import guideme.navigation.NavigationNode;
import guideme.navigation.NavigationTree;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Patches the guide navigation tree so sidebar titles follow {@link TheRootsGuideNames}.
 */
public final class TheRootsNavigationPatcher {
    private static final ModLogger LOGGER = ModLogger.of(TheRootsNavigationPatcher.class);
    static final Identifier GUIDE_ID = Identifier.fromNamespaceAndPath("iska_utils", "guide");
    private static final Identifier THE_ROOTS_PAGE = Identifier.fromNamespaceAndPath("iska_utils", "items/the_roots.md");

    private TheRootsNavigationPatcher() {
    }

    public static void patchGuide(@Nullable Guide guide) {
        if (guide == null) {
            return;
        }
        var patched = patchTree(guide.getNavigationTree());
        if (patched != null) {
            setNavigationTree(guide, patched);
        }
    }

    @Nullable
    private static NavigationTree patchTree(NavigationTree tree) {
        var patchedRoots = tree.getRootNodes().stream().map(TheRootsNavigationPatcher::patchNode).toList();
        var index = new HashMap<Identifier, NavigationNode>();
        for (var root : patchedRoots) {
            collectIndex(root, index);
        }
        return new NavigationTree(Map.copyOf(index), List.copyOf(patchedRoots));
    }

    private static NavigationNode patchNode(NavigationNode node) {
        var title = isTheRootsPage(node.pageId()) ? TheRootsGuideNames.displayName() : node.title();
        var children = node.children().stream().map(TheRootsNavigationPatcher::patchNode).toList();
        return new NavigationNode(node.pageId(), title, node.icon(), children, node.position(), node.hasPage());
    }

    private static void collectIndex(NavigationNode node, Map<Identifier, NavigationNode> index) {
        if (node.pageId() != null) {
            index.put(node.pageId(), node);
        }
        for (var child : node.children()) {
            collectIndex(child, index);
        }
    }

    private static boolean isTheRootsPage(@Nullable Identifier pageId) {
        return THE_ROOTS_PAGE.equals(pageId);
    }

    private static void setNavigationTree(Guide guide, NavigationTree tree) {
        try {
            Field field = guide.getClass().getDeclaredField("navigationTree");
            field.setAccessible(true);
            field.set(guide, tree);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Could not patch The Roots navigation title: {}", e.toString());
        }
    }
}
