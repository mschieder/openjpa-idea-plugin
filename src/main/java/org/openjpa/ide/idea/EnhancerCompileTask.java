package org.openjpa.ide.idea;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileTask;
import org.jetbrains.annotations.NotNull;

public class EnhancerCompileTask implements CompileTask {

    private Computable dNEComputable;


    private void lazyInit(CompileContext context) {
        if (dNEComputable == null) {
            dNEComputable = EnhancerService.getInstance(context.getProject()).getdNEComputable();
        }
    }

    @Override
    public boolean execute(@NotNull CompileContext context) {
        lazyInit(context);
        dNEComputable.process(context, dNEComputable.getProcessingItems(context));
        return true;
    }
}
