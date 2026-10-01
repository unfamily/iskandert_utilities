package net.unfamily.iskautils.guide;

import guideme.compiler.PageCompiler;
import guideme.compiler.tags.BlockTagCompiler;
import guideme.document.block.LytBlockContainer;
import guideme.libs.mdast.mdx.model.MdxJsxElementFields;
import java.util.Set;
import net.unfamily.iskautils.Config;

/**
 * Conditionally renders guide blocks depending on {@link Config#shouldRegisterSilverfishLarva()}.
 *
 * <p>{@code <iska_utils:IfSilverfishLarva>...</iska_utils:IfSilverfishLarva>}
 * <p>{@code <iska_utils:UnlessSilverfishLarva>...</iska_utils:UnlessSilverfishLarva>}
 */
public final class SilverfishLarvaConditionalTagCompiler extends BlockTagCompiler {
    private static final String IF = "iska_utils:IfSilverfishLarva";
    private static final String UNLESS = "iska_utils:UnlessSilverfishLarva";

    @Override
    public Set<String> getTagNames() {
        return Set.of(IF, UNLESS);
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        boolean registered = Config.shouldRegisterSilverfishLarva();
        boolean show = IF.equals(el.name()) ? registered : !registered;
        if (show) {
            compiler.compileBlockContext(el.children(), parent);
        }
    }
}
