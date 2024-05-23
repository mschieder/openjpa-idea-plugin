package org.openjpa.ide.idea;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Holds plugin's persistent state.
 */
@Service(Service.Level.PROJECT)
@com.intellij.openapi.components.State(name = "OpenJpaConfiguration",
        storages = {@Storage(value = "openjpa-plugin.xml")})
public final class PersistentState implements PersistentStateComponent<PersistentState> { // has to be public (for IDEA configuration access)

    private boolean enhancerEnabled = true;

    @XCollection(elementTypes = String.class)
    private Collection<String> metaDataExtensions = new ArrayList<>(Arrays.asList("jdo", "orm"));

    /**
     * Indicator if {@link #metaDataExtensions} should be added to compiler resource patterns
     */
    private boolean addToCompilerResourcePatterns = true;

    private boolean includeTestClasses = true;
    private boolean addDefaultConstructor = true;
    private boolean enforcePropertyRestrictions = true;
    private boolean tmpClassLoader = true;

    @XCollection(elementTypes = String.class)
    private Collection<String> enabledModules = new ArrayList<>();

    @XCollection(elementTypes = String.class)
    private Collection<String> enabledFiles = new ArrayList<>();

    private String api = "JPA";

    private String enhancerSupport = "OPENJPA";

    public boolean isEnhancerEnabled() {
        return this.enhancerEnabled;
    }

    public void setEnhancerEnabled(final boolean enhancerEnabled) {
        this.enhancerEnabled = enhancerEnabled;
    }

    public Collection<String> getMetaDataExtensions() {
        return new LinkedHashSet<>(this.metaDataExtensions);
    }

    public void setMetaDataExtensions(final Collection<String> metaDataExtensions) {
        this.metaDataExtensions = new LinkedHashSet<>(metaDataExtensions);
    }

    public boolean isAddToCompilerResourcePatterns() {
        return this.addToCompilerResourcePatterns;
    }

    public void setAddToCompilerResourcePatterns(final boolean addToCompilerResourcePatterns) {
        this.addToCompilerResourcePatterns = addToCompilerResourcePatterns;
    }

    public boolean isIncludeTestClasses() {
        return this.includeTestClasses;
    }

    public void setIncludeTestClasses(final boolean includeTestClasses) {
        this.includeTestClasses = includeTestClasses;
    }

    public Collection<String> getEnabledModules() {
        return new LinkedHashSet<>(this.enabledModules);
    }

    public void setEnabledModules(final Collection<String> enabledModules) {
        this.enabledModules = new LinkedHashSet<>(enabledModules);
    }

    public Collection<String> getEnabledFiles() {
        return new LinkedHashSet<>(this.enabledFiles);
    }

    public void setEnabledFiles(final Collection<String> enabledFiles) {
        this.enabledFiles = new LinkedHashSet<>(enabledFiles);
    }

    public String getApi() {
        return this.api;
    }

    public void setApi(final String api) {
        this.api = api;
    }

    public String getEnhancerSupport() {
        return this.enhancerSupport;
    }

    public void setEnhancerSupport(final String enhancerSupport) {
        this.enhancerSupport = enhancerSupport;
    }


    public boolean isAddDefaultConstructor() {
        return addDefaultConstructor;
    }

    public void setAddDefaultConstructor(boolean addDefaultConstructor) {
        this.addDefaultConstructor = addDefaultConstructor;
    }

    public boolean isEnforcePropertyRestrictions() {
        return enforcePropertyRestrictions;
    }

    public void setEnforcePropertyRestrictions(boolean enforcePropertyRestrictions) {
        this.enforcePropertyRestrictions = enforcePropertyRestrictions;
    }

    public boolean isTmpClassLoader() {
        return tmpClassLoader;
    }

    public void setTmpClassLoader(boolean tmpClassLoader) {
        this.tmpClassLoader = tmpClassLoader;
    }

    @Override
    public PersistentState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PersistentState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
