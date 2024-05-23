package org.openjpa.ide.idea;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.openjpa.ide.idea.integration.EnhancerSupport;

@Service(Service.Level.PROJECT)
public final class State {

    private final EnhancerSupportRegistry enhancerSupportRegistry = EnhancerSupportRegistryDefault.getInstance();

    private final PersistentState persistentState;

    public static State getInstance(Project project) {
        return project.getService(State.class);
    }

    public State(Project project) {
        this.persistentState = project.getService(PersistentState.class);
    }

    public boolean isEnhancerEnabled() {
        return persistentState.isEnhancerEnabled();
    }

    public void setEnhancerEnabled(final boolean enhancerEnabled) {
       persistentState.setEnhancerEnabled(enhancerEnabled);
    }

    public Set<String> getMetaDataExtensions() {
        return new LinkedHashSet<>(persistentState.getMetaDataExtensions());
    }

    public void setMetaDataExtensions(final Collection<String> metaDataExtensions) {
        persistentState.setMetaDataExtensions(metaDataExtensions);
    }

    public boolean isAddToCompilerResourcePatterns() {
        return persistentState.isAddToCompilerResourcePatterns();
    }

    public void setAddToCompilerResourcePatterns(final boolean addToCompilerResourcePatterns) {
        persistentState.setAddToCompilerResourcePatterns(addToCompilerResourcePatterns);
    }

    public boolean isIncludeTestClasses() {
        return persistentState.isIncludeTestClasses();
    }

    public void setIncludeTestClasses(final boolean includeTestClasses) {
        persistentState.setIncludeTestClasses(includeTestClasses);
    }

    public boolean isAddDefaultConstructor() {
        return persistentState.isAddDefaultConstructor();
    }

    public void setAddDefaultConstructor(boolean addDefaultConstructor) {
        persistentState.setAddDefaultConstructor(addDefaultConstructor);
    }

    public boolean isEnforcePropertyRestrictions() {
        return persistentState.isEnforcePropertyRestrictions();
    }

    public void setEnforcePropertyRestrictions(boolean enforcePropertyRestrictions) {
        persistentState.setEnforcePropertyRestrictions(enforcePropertyRestrictions);
    }

    public boolean isTmpClassLoader() {
        return persistentState.isTmpClassLoader();
    }

    public void setTmpClassLoader(boolean tmpClassLoader) {
        persistentState.setTmpClassLoader(tmpClassLoader);
    }

    public Set<String> getEnabledModules() {
        return new LinkedHashSet<>(persistentState.getEnabledModules());
    }

    public Set<String> getEnabledFiles() {
        return new LinkedHashSet<>(persistentState.getEnabledFiles());
    }

    public void setEnabledModules(final Collection<String> enabledModules) {
        persistentState.setEnabledModules(enabledModules);
    }

    public boolean isEnhanceAllPersistentClasses(){
        return persistentState.isEnhanceAllPersistentClasses();
    }

    public void setEnhanceAllPersistentClasses(boolean enhanceAllPersistentClasses){
        persistentState.setEnhanceAllPersistentClasses(enhanceAllPersistentClasses);
    }

    public void setEnabledFiles(final Collection<String> files) {
        persistentState.setEnabledFiles(files);
    }

    public EnhancerSupportRegistry getEnhancerSupportRegistry() {
        return this.enhancerSupportRegistry;
    }

    public EnhancerSupport getEnhancerSupport() {
        final EnhancerSupportRegistry eSR = this.enhancerSupportRegistry;
        final String enhancerSupportString = persistentState.getEnhancerSupport();
        final EnhancerSupport myEnhancerSupport;
        if (enhancerSupportString == null || enhancerSupportString.trim().isEmpty() || !eSR.isRegistered(enhancerSupportString)) {
            myEnhancerSupport = eSR.getDefaultEnhancerSupport();
        } else {
            myEnhancerSupport = eSR.getEnhancerSupportById(enhancerSupportString);
        }

        return myEnhancerSupport;
    }

    public void setEnhancerSupport(final EnhancerSupport enhancerSupport) {
        persistentState.setEnhancerSupport(enhancerSupport != null ? enhancerSupport.getId() : null);
    }

    public PersistenceApi getApi() {
        return PersistenceApi.valueOf(persistentState.getApi());
    }

    public void setApi(final PersistenceApi api) {
        persistentState.setApi(api != null ? api.name() : null);
    }

}
