package org.openjpa.ide.idea;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.openjpa.ide.idea.config.AffectedModule;
import org.openjpa.ide.idea.config.ConfigForm;
import org.openjpa.ide.idea.config.GuiState;
import org.openjpa.ide.idea.config.MetaDataOrClassFile;
import org.openjpa.ide.idea.integration.EnhancerSupport;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides controller functionality for application settings.
 */
final class ProjectConfigurable implements Configurable {

    private ConfigForm configGuiForm = null;
    private final Project project;
    private final State state;
    private final EnhancerService enhancerService;

    private static final String RESOURCE_PATTERN_PREFIX = "?*.";

    private static final Pattern REPLACE_PATTERN_WILDCARD_ALL = Pattern.compile("\\*");

    private static final Pattern REPLACE_PATTERN_WILDCARD_DOT = Pattern.compile("\\.");

    private static final Pattern PATTERN_EXTENSION_SEPARATOR = Pattern.compile(";");

    public ProjectConfigurable(Project project){
        this.project = project;
        this.enhancerService = EnhancerService.getInstance(project);
        this.state = State.getInstance(project);

    }

    //
    // Configurable interface implementation
    //

    @Nls
    @Override
    public String getDisplayName() {
        return "OpenJpa Enhancer";
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public JComponent createComponent() {
        if (this.configGuiForm == null) {
            this.configGuiForm = new ConfigForm();
        }
        return this.configGuiForm.getRootComponent();
    }

    @Override
    public boolean isModified() {
        return this.configGuiForm.isModified();
    }

    @SuppressWarnings("FeatureEnvy")
    @Override
    public void apply() {
        final GuiState guiState = new GuiState(this.state.getEnhancerSupportRegistry());
        this.configGuiForm.getData(guiState);
        this.setGuiState(guiState);

        //
        // update compiler resource patterns
        if (this.state.isAddToCompilerResourcePatterns()) {
            final Collection<String> metadataExtensions = this.state.getMetaDataExtensions();
            if (!metadataExtensions.isEmpty()) {
                final CompilerConfiguration cConfig = CompilerConfiguration.getInstance(this.project);
                for (final String metadataExtension : metadataExtensions) {
                    if (!cConfig.isResourceFile("test." + metadataExtension)) {
                        cConfig.addResourceFilePattern(RESOURCE_PATTERN_PREFIX + metadataExtension);
                    }
                }
            }
        }

        this.reset();
    }

    @Override
    public void reset() {
        this.configGuiForm.setData(this.getGuiState());
    }

    @Override
    public void disposeUIResources() {
        this.configGuiForm = null;
    }

    //
    // Gui interface
    //

    @SuppressWarnings("FeatureEnvy")
    private GuiState getGuiState() {
        boolean indexReady = false;
        final boolean enhancerEnabled = this.state.isEnhancerEnabled();
        final String metaDataExtension = getMetaDataExtensionsString(this.state.getMetaDataExtensions());
        final boolean addToCompilerResourcePatterns = this.state.isAddToCompilerResourcePatterns();
        final boolean includeTestClasses = this.state.isIncludeTestClasses();
        final boolean addDefaultConstructor = this.state.isAddDefaultConstructor();
        final boolean enforcePropertyRestrictions = this.state.isEnforcePropertyRestrictions();
        final boolean tmpClassLoader = this.state.isTmpClassLoader();
        final boolean enhancerInitialized = enhancerService.isEnhancerInitialized();
        final PersistenceApi api = this.state.getApi();
        final EnhancerSupport enhancerSupport = this.state.getEnhancerSupport();
        List<AffectedModule> affectedModules;
        List<MetaDataOrClassFile> metaDataFiles;
        List<MetaDataOrClassFile> annotatedClassFiles;
        try {
            affectedModules = this.getAffectedModulesGuiModel();
            metaDataFiles = this.createMetadataFilesGuiModel();
            // filter files:
            annotatedClassFiles = this.createAnnotatedClassFilesGuiModel();
            if (this.state.getEnabledFiles().size() > 0) {
                applyFilter(annotatedClassFiles, this.state.getEnabledFiles());
            }
            indexReady = true;
        } catch (IndexNotReadyException ignored) {
            affectedModules = new ArrayList<AffectedModule>(0);
            metaDataFiles = new ArrayList<MetaDataOrClassFile>(0);
            annotatedClassFiles = new ArrayList<MetaDataOrClassFile>(0);
        }
        return new GuiState(indexReady,
                enhancerEnabled,
                metaDataExtension,
                addToCompilerResourcePatterns,
                includeTestClasses,
                addDefaultConstructor,
                enforcePropertyRestrictions,
                tmpClassLoader,
                enhancerInitialized,
                api,
                this.state.getEnhancerSupportRegistry(),
                enhancerSupport,
                affectedModules,
                metaDataFiles,
                annotatedClassFiles);
    }

    private void applyFilter(List<MetaDataOrClassFile> annotatedClassFiles, Set<String> enabledFiles) {
        for (MetaDataOrClassFile file : annotatedClassFiles) {
            if (enabledFiles.contains(file.getClassName())) {
                file.setEnabled(true);
            } else {
                file.setEnabled(false);
            }
        }
    }

    @SuppressWarnings("FeatureEnvy")
    private void setGuiState(final GuiState guiState) {
        final boolean enhancerEnabled = guiState.isEnhancerEnabled();
        final LinkedHashSet<String> metaDataExtensions = getMetaDataExtensionsSet(guiState.getMetaDataExtensions());
        final boolean addToCompilerResourcePatterns = guiState.isAddToCompilerResourcePatterns();
        final boolean includeTestClasses = guiState.isIncludeTestClasses();
        final boolean addDefaultConstructor = guiState.isAddDefaultConstructor();
        final boolean enforcePropertyRestrictions = guiState.isEnforcePropertyRestrictions();
        final boolean tmpClassLoader = guiState.isTmpClassLoader();
        final PersistenceApi api = guiState.getApi();
        final EnhancerSupport enhancerSupport = guiState.getEnhancerSupport();
        final Set<String> enabledFiles = getEnabledFilesFromGuiModel(guiState.getMetadataFiles());
        final Set<String> enabledModules = getEnabledModulesFromGuiModel(guiState.getAffectedModules());

        this.state.setEnhancerEnabled(enhancerEnabled);
        this.state.setMetaDataExtensions(metaDataExtensions);
        this.state.setAddToCompilerResourcePatterns(addToCompilerResourcePatterns);
        this.state.setIncludeTestClasses(includeTestClasses);
        this.state.setAddDefaultConstructor(addDefaultConstructor);
        this.state.setEnforcePropertyRestrictions(enforcePropertyRestrictions);
        this.state.setTmpClassLoader(tmpClassLoader);
        this.state.setEnabledModules(enabledModules);
        this.state.setEnabledFiles(enabledFiles);
        this.state.setApi(api);
        this.state.setEnhancerSupport(enhancerSupport);


        // TODO: hack to filter modules not supported by enhancer (filtering only possible after updating the state with enhancer settings)
        this.filterEnhancerSupportedModules();

        this.filterEnhancerSupportedFiles();
    }

    private Set<String> getEnabledFilesFromGuiModel(List<MetaDataOrClassFile> metadataFiles) {
        final Set<String> enableFiles = new HashSet<String>();
        if (metadataFiles != null) {
            for (final MetaDataOrClassFile file : metadataFiles) {
                if (file.isEnabled()) {
                    enableFiles.add(file.getClassName());
                }
            }
        }
        return enableFiles;
    }


    //
    // Gui model helper methods
    //

    private void filterEnhancerSupportedModules() {

        // TODO: hack to filter modules not supported by enhancer (filtering only possible after updating the state with enhancer settings)
        final List<AffectedModule> affectedModulesGuiModel = getAffectedModulesGuiModel();
        final Collection<String> filter = new HashSet<String>(affectedModulesGuiModel.size());
        for (final AffectedModule affectedModule : affectedModulesGuiModel) {
            filter.add(affectedModule.getName());
        }

        final Collection<String> enhancerSupportedModules = new LinkedHashSet<String>(affectedModulesGuiModel.size());
        for (final String enabledModule : this.state.getEnabledModules()) {
            if (filter.contains(enabledModule)) {
                enhancerSupportedModules.add(enabledModule);
            }
        }

        this.state.setEnabledModules(enhancerSupportedModules);
    }

    private void filterEnhancerSupportedFiles() {
        final List<MetaDataOrClassFile> meta = createAnnotatedClassFilesGuiModel();
        final Collection<String> filter = new HashSet<String>(meta.size());
        for (final MetaDataOrClassFile file : meta) {
            filter.add(file.getClassName());
        }

        final Collection<String> enhancerSupportedFiles = new LinkedHashSet<String>(meta.size());
        for (final String fileName : this.state.getEnabledFiles()) {
            if (filter.contains(fileName)) {
                enhancerSupportedFiles.add(fileName);
            }
        }

        this.state.setEnabledFiles(enhancerSupportedFiles);
    }


    private List<AffectedModule> getAffectedModulesGuiModel() {
        final List<AffectedModule> moduleList = new ArrayList<AffectedModule>();
        final List<Module> affectedModules = IdeaProjectUtils.getDefaultAffectedModules(this.state.getEnhancerSupport(), this.project);

        for (final Module module : affectedModules) {
            final Set<String> enabledModules = this.state.getEnabledModules();
            final boolean enabled = enabledModules != null && enabledModules.contains(module.getName());
            moduleList.add(new AffectedModule(enabled, module.getName()));
        }
        return moduleList;
    }

    private static Set<String> getEnabledModulesFromGuiModel(final Iterable<AffectedModule> affectedModules) {
        final Set<String> enabledModules = new HashSet<String>();
        if (affectedModules != null) {
            for (final AffectedModule affectedModule : affectedModules) {
                if (affectedModule.isEnabled()) {
                    enabledModules.add(affectedModule.getName());
                }
            }
        }
        return enabledModules;
    }

    private List<MetaDataOrClassFile> createMetadataFilesGuiModel() {
       final Map<Module, List<VirtualMetadataFile>> metaDataFiles =
                this.enhancerService.getMetadataFiles();
        return createFilesGuiModel(metaDataFiles);
    }

    private List<MetaDataOrClassFile> createAnnotatedClassFilesGuiModel() {
        final Map<Module, List<VirtualMetadataFile>> annotatedClassFiles =
                this.enhancerService.getAnnotatedClassFiles();
        return createFilesGuiModel(annotatedClassFiles);
    }

    @SuppressWarnings("FeatureEnvy")
    private static List<MetaDataOrClassFile> createFilesGuiModel(final Map<Module,
            List<VirtualMetadataFile>> metaDataOrAnnotatedClassFiles) {

        final List<MetaDataOrClassFile> metaDataOrClassFiles = new ArrayList<MetaDataOrClassFile>();
        for (final Map.Entry<Module, List<VirtualMetadataFile>> moduleListEntry : metaDataOrAnnotatedClassFiles.entrySet()) {
            for (final VirtualMetadataFile vf : moduleListEntry.getValue()) {
                for (final String mfClassName : vf.getClassNames()) {
                    final Module moduleListEntryKey = moduleListEntry.getKey();
                    metaDataOrClassFiles.add(new MetaDataOrClassFile(moduleListEntryKey.getName(),
                            vf.getDisplayFilename(),
                            vf.getDisplayPath(),
                            mfClassName, true));
                }
            }
        }
        return metaDataOrClassFiles;
    }

    private static String getMetaDataExtensionsString(final Collection<String> extensions) {
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        for (final String extension : extensions) {
            sb.append(extension);
            ++count;
            if (count < extensions.size()) {
                sb.append("; ");
            }
        }
        return sb.toString();
    }

    private static LinkedHashSet<String> getMetaDataExtensionsSet(final String extensions) {
        final LinkedHashSet<String> retExtensions = new LinkedHashSet<String>();
        if (extensions != null && !extensions.isEmpty()) {
            final Matcher replacePatternWildCardAll = REPLACE_PATTERN_WILDCARD_ALL.matcher(extensions);
            final Matcher replacePatternWildCardDot = REPLACE_PATTERN_WILDCARD_DOT.matcher(replacePatternWildCardAll.replaceAll(""));
            final String cleanedExtensions = replacePatternWildCardDot.replaceAll("");
            final String[] rawExtensions = PATTERN_EXTENSION_SEPARATOR.split(cleanedExtensions);
            for (final String rawExtension : rawExtensions) {
                retExtensions.add(rawExtension.trim());
            }
        }
        return retExtensions;
    }

}
