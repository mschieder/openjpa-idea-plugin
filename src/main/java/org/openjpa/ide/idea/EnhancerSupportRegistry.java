package org.openjpa.ide.idea;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.openjpa.ide.idea.integration.EnhancerSupport;

/**
 * Registry of supported enhancers
 */
public interface EnhancerSupportRegistry {

    @NotNull
    EnhancerSupport getEnhancerSupportById(@NotNull String id);

    boolean isRegistered(@NotNull String id);

    @NotNull
    EnhancerSupport getDefaultEnhancerSupport();

    @NotNull
    Set<EnhancerSupport> getSupportedEnhancers();

    void registerEnhancerSupport(@NotNull EnhancerSupport enhancerSupport);

    void unRegisterEnhancerSupport(@NotNull EnhancerSupport enhancerSupport);

}
