package net.unfamily.iskautils.guide;

import guideme.compiler.PageCompiler;
import guideme.compiler.tags.BlockTagCompiler;
import guideme.document.block.LytBlockContainer;
import guideme.document.block.LytHeading;
import guideme.libs.mdast.mdx.model.MdxJsxElementFields;
import java.util.Set;

/**
 * Renders the page H1 for The Roots with the same singular/plural rules as the item name.
 *
 * <p>Markdown: {@code <iska_utils:TheRootsTitle />}
 */
public final class TheRootsTitleTagCompiler extends BlockTagCompiler {
    @Override
    public Set<String> getTagNames() {
        return Set.of("iska_utils:TheRootsTitle");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        var heading = new LytHeading();
        heading.setDepth(1);
        heading.appendText(TheRootsGuideNames.displayName());
        parent.append(heading);
    }
}
