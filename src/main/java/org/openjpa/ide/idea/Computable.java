package org.openjpa.ide.idea;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.compiler.SourceInstrumentingCompiler;
import com.intellij.openapi.compiler.TimestampValidityState;
import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openjpa.ide.idea.integration.EnhancerProxy;
import org.openjpa.ide.idea.integration.EnhancerSupport;

import java.io.DataInput;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enhances class files with xml- or annotation based metadata in
 * all build-cycle affected project modules where xml metadata or
 * annotated classes could be found.<br>
 * <br>
 * Activated enhancer integration dependencies must be in the project module's classpath!<br>
 * If not, although activated, the module will be ignored and a warning will be logged (Idea messages).<br>
 * <br>
 * This implementation always tries to enhance all files in a module at once, hence a failing
 * class can prevent all others (also whole modules) from being processed.<br>
 * <br>
 * Failure stacktraces are transformed into strings and written to the idea messages output.
 */
class Computable implements SourceInstrumentingCompiler {

    private static final ProcessingItem[] EMPTY_PROCESSING_ITEMS = new ProcessingItem[0];
    private static final String CLASSFILE_EXTENSION = ".class";
    //
    // Members
    //

    /**
     * Current project
     */
    private final Project project;

    /**
     * Plugin shared configuration
     */
    private final State state;

    //
    // Constructor
    //

    Computable(final Project project, final State state) {
        this.project = project;
        this.state = state;
    }

    //
    // ClassPostProcessingCompiler interface implementation
    //

    @NotNull
    @Override
    public ProcessingItem[] getProcessingItems(final @NotNull CompileContext compileContext) {
        final Set<String> enabledModules = this.state.getEnabledModules();
        if (this.state.isEnhancerEnabled() && !enabledModules.isEmpty()) {
            // get metadata files of affected modules
            final Map<Module, List<VirtualMetadataFile>> moduleBasedMetadataFiles =
                    this.getMetadataFiles(compileContext.getCompileScope());

            // get annotated class files of affected modules
            final Map<Module, List<VirtualMetadataFile>> moduleBasedAnnotatedClasses =
                    this.getAnnotatedClassFiles(compileContext.getCompileScope());

            final Collection<ProcessingItem> processingItems =
                    new LinkedHashSet<>();
            for (final List<VirtualMetadataFile> metadataFileList : moduleBasedMetadataFiles.values()) {
                for (final VirtualMetadataFile virtualMetadataFile : metadataFileList) {
                    final Collection<EnhancerItem> enhancerItems = virtualMetadataFile.toEnhancerItems();
                    processingItems.addAll(enhancerItems);
                }
            }
            for (final List<VirtualMetadataFile> annotatedClassesFileList : moduleBasedAnnotatedClasses.values()) {
                for (final VirtualMetadataFile virtualMetadataFile : annotatedClassesFileList) {
                    final Collection<EnhancerItem> enhancerItems = virtualMetadataFile.toEnhancerItems();
                    processingItems.addAll(enhancerItems);
                }
            }

            if (processingItems.isEmpty()) {
                this.logMessage(compileContext,
                        CompilerMessageCategory.WARNING,
                        "Enhancer: no metadata- or annotated class-files found");
            }

            return processingItems.toArray(new ProcessingItem[0]);
        } else {
            return EMPTY_PROCESSING_ITEMS;
        }
    }

    @Override
    public ProcessingItem[] process(final @NotNull CompileContext ctx,
                                    final ProcessingItem @NotNull [] processingItems) {

        org.apache.log4j.BasicConfigurator.configure();
        ProcessingItem[] ret = EMPTY_PROCESSING_ITEMS;

        // shortcut if disabled
        final Set<String> enabledModules = this.state.getEnabledModules();
        if (this.state.isEnhancerEnabled() && !enabledModules.isEmpty()) {

            // just to be sure: backup of classloader
            final ClassLoader previousCL = Thread.currentThread().getContextClassLoader();

            // for displaying progress messages
            final ProgressIndicator progressIndicator = ctx.getProgressIndicator();

            try {
                // GUI State display init
                progressIndicator.pushState();
                progressIndicator.setText("OpenJpa Enhancer running");

                final LinkedHashMap<Module, List<VirtualMetadataFile>> moduleBasedMetadataFiles =
                        new LinkedHashMap<>();

                final LinkedHashMap<Module, List<VirtualMetadataFile>> moduleBasedAnnotatedClasses =
                        new LinkedHashMap<>();

                for (final ProcessingItem processingItem : processingItems) {
                    final EnhancerItem enhancerItem = (EnhancerItem) processingItem;
                    final VirtualMetadataFile virtualMetadata = enhancerItem.getVirtualMetadata();
                    final Module module = virtualMetadata.getModule();
                    if (virtualMetadata.isAnnotationBasedOnly()) {
                        List<VirtualMetadataFile> annotatedClassesList = moduleBasedAnnotatedClasses.computeIfAbsent(module, k -> new ArrayList<>());
                        annotatedClassesList.add(virtualMetadata);
                    } else {
                        List<VirtualMetadataFile> metadataFileList = moduleBasedMetadataFiles.computeIfAbsent(module, k -> new ArrayList<>());
                        if (!metadataFileList.contains(virtualMetadata)) {
                            metadataFileList.add(virtualMetadata);
                        }
                    }
                }

                // detect all modules affected
                final List<Module> affectedModules = getAffectedModules(ctx, moduleBasedMetadataFiles, moduleBasedAnnotatedClasses);


                // no metadata or annotated classes -> no enhancement
                if (!affectedModules.isEmpty()) {

                    // start enhancer per module
                    final int count = enhanceInModules(ctx, affectedModules, moduleBasedMetadataFiles, moduleBasedAnnotatedClasses);
                    // success message
                    this.logMessage(ctx,
                            CompilerMessageCategory.INFORMATION,
                            "Enhancer: Successfully enhanced " + count + " classes");
                } else {
                    this.logMessage(ctx,
                            CompilerMessageCategory.WARNING,
                            "Enhancer: no Hibernate/JPA metadata or annotated class files found");
                }

                ret = processingItems;

            } catch (Exception e) {
                // writer for stacktrace printing
                final Writer writer = new StringWriter();
                // transform stacktrace to string
                final PrintWriter printWriter = new PrintWriter(writer);
                try {
                    e.printStackTrace(printWriter);
                } finally {
                    printWriter.close();
                }
                // write stacktrace string to messages
                this.logMessage(ctx,
                        CompilerMessageCategory.ERROR,
                        "An unexpected error occurred in the OpenJpa plugin. Stacktrace: " + "\n\n" + writer);
            } finally {
                Thread.currentThread().setContextClassLoader(previousCL);
                progressIndicator.popState();
            }
        }

        return ret;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "OpenJpa Enhancer";
    }

    @Override
    public boolean validateConfiguration(final CompileScope compileScope) {
        return true;
    }

    @Override
    public ValidityState createValidityState(final DataInput dataInput) throws IOException {
        return TimestampValidityState.load(dataInput);
    }

    //
    // Helper methods
    //

    @SuppressWarnings("FeatureEnvy")
    private int enhanceInModules(final CompileContext ctx,
                                 final Iterable<Module> affectedModules,
                                 final LinkedHashMap<Module, List<VirtualMetadataFile>> moduleBasedMetadataFiles,
                                 final LinkedHashMap<Module, List<VirtualMetadataFile>> moduleBasedAnnotatedClasses)
            throws IllegalAccessException,
            InstantiationException, InvocationTargetException, NoSuchFieldException {

        int count = 0;
        for (final Module module : affectedModules) {
            // exclude disabled modules
            final Set<String> enabledModules = this.state.getEnabledModules();
            if (enabledModules.contains(module.getName())) {

                // get modules output folder
                final VirtualFile outputDirectory = ctx.getModuleOutputDirectory(module);

                // only enhance in modules that have an output folder
                if (outputDirectory == null) {
                    // display warning if module has no output folder
                    this.logMessage(ctx,
                            CompilerMessageCategory.WARNING,
                            "Enhancer: no output directory for module: " + module.getName());

                } else {
                    final ProgressIndicator progressIndicator = ctx.getProgressIndicator();
                    final EnhancerSupport enhancerSupport = this.state.getEnhancerSupport();

                    // update progress text
                    progressIndicator.setText(enhancerSupport.getName() + " Enhancer enhancing in " + module.getName());
                    // metadata files for module
                    final List<VirtualMetadataFile> metadataFiles = moduleBasedMetadataFiles.get(module);
                    // metadata files for module
                    final List<VirtualMetadataFile> annotatedClassFiles = moduleBasedAnnotatedClasses.get(module);

                    try {
                        // do class enhancement in module
                        count += enhancePerModule(enhancerSupport,
                                this.state,
                                ctx,
                                module,
                                outputDirectory,
                                metadataFiles,
                                annotatedClassFiles);
                    } catch (ClassNotFoundException ignored) {
                        this.logMessage(ctx,
                                CompilerMessageCategory.WARNING,
                                "Enhancer: enhancer not found in classpath for module: " + module.getName());
                    } catch (NoSuchMethodException ignored) {
                        this.logMessage(ctx,
                                CompilerMessageCategory.ERROR,
                                "Enhancer: enhancer mehtod not found for module: " + module.getName());
                    }
                }
            }
        }
        return count;
    }

    @SuppressWarnings("FeatureEnvy")
    private static int enhancePerModule(final EnhancerSupport enhancerSupport,
                                        final State state,
                                        final CompileContext compileContext,
                                        final Module module,
                                        final VirtualFile outputDirectory,
                                        final Collection<VirtualMetadataFile> metadataFiles,
                                        final Collection<VirtualMetadataFile> annotatedClassFiles)
            throws ClassNotFoundException,
            IllegalAccessException,
            InstantiationException,
            InvocationTargetException,
            NoSuchMethodException, NoSuchFieldException {

        //
        // what to enhance

        // metadata based classes
        final boolean metadataBased = metadataFiles != null && !metadataFiles.isEmpty();
        // annotation based classes
        final boolean annotationBased = annotatedClassFiles != null && !annotatedClassFiles.isEmpty();

        //
        // only enhance if metadata or annotation based class files present

        final boolean doEnhance = metadataBased || annotationBased;

        //
        // create enhancer instance

        final EnhancerProxy enhancer;
        if (doEnhance) {

            enhancer = enhancerSupport.newEnhancerProxy(state.getApi(), compileContext, module, null);
            enhancer.setAddDefaultConstructor(state.isAddDefaultConstructor());
            enhancer.setEnforcePropertyRestrictions(state.isEnforcePropertyRestrictions());

        } else {
            // nothing to enhance
            return 0;
        }

        //
        // add metadata and classes

        // add metadata based classes to enhancer list
        if (metadataBased) {

            // iterate modules and enhance classes in corresponding output folders
            for (final VirtualMetadataFile metadataFile : metadataFiles) {

                // get metadata file url
                final VirtualFile metadataVirtualFile = metadataFile.getFile();
                final String metadataFilePath = metadataVirtualFile.getPath();
                // add metadata file url to enhancer
                enhancer.addMetadataFiles(metadataFilePath);

                // parse package and class names
                final Collection<String> classNames = metadataFile.getClassNames();

                // add xml metadata based classes
                for (final String className : classNames) {
                    final String classNameAsPath = IdeaProjectUtils.packageToPath(className);
                    final String fullPath = outputDirectory.getPath() + '/' + classNameAsPath + CLASSFILE_EXTENSION;

                    enhancer.addClasses(fullPath);
                }
            }
        }

        // add annotated classes to enhancer list
        if (annotationBased) {

            for (final VirtualMetadataFile annotatedClassFile : annotatedClassFiles) {
                final VirtualFile annotatedClassVirtualFile = annotatedClassFile.getFile();
                enhancer.addClasses(annotatedClassVirtualFile.getPath());
            }
        }

        //
        // finally enhance classes

        // count nr of enhanced classes
        final int enhancedCount;

        // finally enhance all found classes in module
        enhancedCount = enhancer.enhance();

        return enhancedCount;
    }

    //
    // Utility methods
    //

    /**
     * Retrieve annotated class files.
     *
     * @param compileScope compile scope to use (null for default - can lead to invalid file list due to refactoring)
     * @return .
     */
    // TODO: cleanup, as this seems to be very hacky
    @SuppressWarnings("FeatureEnvy")
    Map<Module, List<VirtualMetadataFile>> getAnnotatedClassFiles(@Nullable final CompileScope compileScope) {
        final LinkedHashMap<Module, List<VirtualMetadataFile>> moduleBasedFiles = new LinkedHashMap<>();

        final Application application = ApplicationManager.getApplication();
        application.runReadAction(() -> {
            final CompileScope projectCompileScope = compileScope == null
                    ? CompilerManager.getInstance(Computable.this.project).createProjectCompileScope(Computable.this.project)
                    : compileScope;

            final Set<String> enabledFiles = Computable.this.state.getEnabledFiles();

            for (final Module module : projectCompileScope.getAffectedModules()) {
                if (Computable.this.state.getEnabledModules().contains(module.getName())) {

                    final List<PsiClass> annotatedClasses = IdeaProjectUtils.findPersistenceAnnotatedClasses(
                            Computable.this.state.getEnhancerSupport(), module);
                    final Collection<VirtualFile> outputDirectories = new ArrayList<>(2);
                    outputDirectories.add(CompilerPaths.getModuleOutputDirectory(module, false));
                    if (Computable.this.state.isIncludeTestClasses()) {
                        outputDirectories.add(CompilerPaths.getModuleOutputDirectory(module, true));
                    }

                    for (final VirtualFile outputDirectory : outputDirectories) {
                        // convert to class files in output directory and add to map
                        if (!annotatedClasses.isEmpty() && outputDirectory != null) {
                            outputDirectory.refresh(true, true);
                            final List<VirtualMetadataFile> moduleFiles = new LinkedList<>();
                            // convert psi classes to class files in output path
                            for (final PsiClass annotatedClass : annotatedClasses) {
                                final String pcClassName = annotatedClass.getQualifiedName();
                                // skip disabled files

                                if (pcClassName == null || (compileScope != null && !enabledFiles.contains(pcClassName))) {
                                    continue;
                                }
                                // convert to path
                                final String pcClassPath = IdeaProjectUtils.packageToPath(pcClassName) + CLASSFILE_EXTENSION;
                                // find file in output path
                                final VirtualFile pcClassFile = outputDirectory.findFileByRelativePath(pcClassPath);
                                if (pcClassFile != null && pcClassFile.exists()) {
                                    moduleFiles
                                            .add(new VirtualMetadataFile(module, true, pcClassFile,
                                                    Collections.singletonList(pcClassName),
                                                    Collections.singletonList(pcClassFile)));
                                }
                            }
                            if (!moduleFiles.isEmpty()) {
                                @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
                                // everything is fine here
                                final List<VirtualMetadataFile> storedModuleFiles = moduleBasedFiles.get(module);
                                // if collection already exists, just add content
                                if (storedModuleFiles == null) {
                                    moduleBasedFiles.put(module, moduleFiles);
                                } else {
                                    storedModuleFiles.addAll(moduleFiles);
                                }
                            }
                        }
                    }
                }
            }
        });

        return moduleBasedFiles;
    }

    /**
     * Retrieve metadata files.
     *
     * @param compileScope compile scope to use (null for default - can lead to invalid file list due to refactoring)
     * @return .
     */
    // TODO: cleanup, as this seems to be very hacky
    @SuppressWarnings("FeatureEnvy")
    Map<Module, List<VirtualMetadataFile>> getMetadataFiles(@Nullable final CompileScope compileScope) {
        final Set<String> extensions;
        if (this.state.getMetaDataExtensions().isEmpty()) {
            extensions = Collections.emptySet(); // State.DEFAULT_METADATA_EXTENSIONS; // no extensions provided -> disable search
        } else {
            extensions = this.state.getMetaDataExtensions();
        }

        final CompileScope projectCompileScope = compileScope == null
                ? CompilerManager.getInstance(this.project).createProjectCompileScope(this.project)
                : compileScope;

        final Map<Module, List<VirtualMetadataFile>> metadataFiles = new LinkedHashMap<>();


        final Application application = ApplicationManager.getApplication();
        application.runReadAction(() -> {
            final Module[] affectedModules = projectCompileScope.getAffectedModules();

            for (final Module module : affectedModules) {
                if (Computable.this.state.getEnabledModules()
                        .contains(module.getName())) {

                    final Collection<VirtualFile> outputDirectories = new ArrayList<>(2);
                    outputDirectories.add(CompilerPaths.getModuleOutputDirectory(module, false));
                    if (Computable.this.state.isIncludeTestClasses()) {
                        outputDirectories.add(CompilerPaths.getModuleOutputDirectory(module, true));
                    }
                    for (final VirtualFile outputDirectory : outputDirectories) {

                        if (outputDirectory != null) {

                            final List<VirtualMetadataFile> moduleFiles = new LinkedList<>();
                            for (final String extension : extensions) {
                                final List<VirtualFile> metadataFilesPerExtension =
                                        IdeaProjectUtils.findFilesByExtension(outputDirectory, extension);

                                // remove non-parseable files
                                for (final VirtualFile vf : metadataFilesPerExtension) {
                                    final Set<String> classNames;
                                    try {
                                        classNames = MetadataParser.parseQualifiedClassNames(vf);
                                    } catch (Exception e) {
                                        throw new IllegalArgumentException("parsing metadata error", e);
                                    }
                                    if (!classNames.isEmpty()) {
                                        final List<VirtualFile> classFiles = new ArrayList<>(classNames.size());
                                        for (final String className : classNames) {
                                            final String classNameAsPath = IdeaProjectUtils.packageToPath(className);
                                            final VirtualFile classFile = outputDirectory.findFileByRelativePath(classNameAsPath + CLASSFILE_EXTENSION);
                                            classFiles.add(classFile);
                                        }

                                        moduleFiles.add(new VirtualMetadataFile(module, false, vf, classNames, classFiles));
                                    }
                                }
                            }
                            if (!moduleFiles.isEmpty()) {

                                metadataFiles.put(module, moduleFiles);
                            }
                        }
                    }
                }
            }
        });
        return metadataFiles;
    }

    static List<Module> getAffectedModules(final CompileContext ctx,
                                           final Map<Module, List<VirtualMetadataFile>> moduleBasedMetadataFiles,
                                           final Map<Module, List<VirtualMetadataFile>> moduleBasedAnnotatedClasses) {
        // list of affected modules
        final List<Module> affectedModules = new ArrayList<>();

        // combine affected module lists (preserving order)
        final CompileScope compileScope = ctx.getCompileScope();
        final Module[] cSAffectedModules = ApplicationManager.getApplication()
                .runReadAction((com.intellij.openapi.util.Computable<Module[]>) compileScope::getAffectedModules);
        for (final Module cSAffectedModule : cSAffectedModules) {
            if (moduleBasedMetadataFiles.containsKey(cSAffectedModule) || moduleBasedAnnotatedClasses.containsKey(cSAffectedModule)) {

                affectedModules.add(cSAffectedModule);
            }
        }
        return affectedModules;


    }

    @SuppressWarnings("MagicCharacter")
    private void logMessage(final CompileContext ctx, final CompilerMessageCategory cat, final String msg) {
        final EnhancerSupport enhancerSupport = this.state.getEnhancerSupport();
        ctx.addMessage(cat, enhancerSupport.getName() + ' ' + msg, null, -1, -1);
    }

}
