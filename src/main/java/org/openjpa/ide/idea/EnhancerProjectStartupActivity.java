package org.openjpa.ide.idea;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openjpa.ide.idea.integration.EnhancerSupport;

public class EnhancerProjectStartupActivity implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        var state = State.getInstance(project);
        final EnhancerSupportRegistry enhancerSupportRegistry = state.getEnhancerSupportRegistry();
        enhancerSupportRegistry.registerEnhancerSupport(EnhancerSupportRegistryDefault.DEFAULT_ENHANCER_SUPPORT);

        for (final EnhancerSupport enhancerSupport : EnhancerSupport.EP_NAME.getExtensions()) {
            enhancerSupportRegistry.registerEnhancerSupport(enhancerSupport);
        }

        return null;
    }
}
