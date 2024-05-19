package org.openjpa.ide.idea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * Abstraction of persistence related metadata.<br/>
 * <br/>
 * Handles metadata originating from classes (annotations) or xml files.<br/>
 * <p/>
 * TODO: seems hacky, do a complete review and cleanup
 */
class VirtualMetadataFile {

    private static final char PATH_SEPARATOR_CHAR = '/';

    private final Module module;

    private final boolean annotationBasedOnly;

    private final VirtualFile file;

    private final Collection<String> classNames;

    private final Collection<VirtualFile> classFiles;

    private final String displayFilename;

    private final String displayPath;

    VirtualMetadataFile(final Module module,
                        final boolean annotationBasedOnly,
                        final VirtualFile file,
                        final Collection<String> classNames,
                        final Collection<VirtualFile> classFiles) {
        Objects.requireNonNull(module, "module is null");
        Objects.requireNonNull(file, "file is null");
        Objects.requireNonNull(classNames, "classNames is null");
        Objects.requireNonNull(classFiles, "classFiles is null");
        this.module = module;
        this.annotationBasedOnly = annotationBasedOnly;
        this.file = file;
        this.classNames = Collections.unmodifiableCollection(classNames);
        this.classFiles = Collections.unmodifiableCollection(classFiles);

        //
        // extract filename and path for displaying purposes

        final String filePath = file.getPath();
        final Project project = module.getProject();
        final VirtualFile projectBaseDir = ProjectUtil.guessProjectDir(project);
        final String projectBaseDirPath = projectBaseDir == null ? null : projectBaseDir.getPath();
        final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        final ModuleRootModel rootModel = moduleRootManager.getModifiableModel();
        final VirtualFile[] contentRoots = rootModel.getContentRoots();

        String pathWithoutModuleName = filePath;
        for (final VirtualFile contentRoot : contentRoots) {
            final String contentRootPath = contentRoot.getPath();
            if (pathWithoutModuleName.contains(contentRootPath)) {
                pathWithoutModuleName =
                        pathWithoutModuleName.substring(pathWithoutModuleName.indexOf(contentRootPath) + contentRootPath.length());
                break;
            }
        }

        if (projectBaseDirPath != null && pathWithoutModuleName.contains(projectBaseDirPath)) {
            pathWithoutModuleName =
                    pathWithoutModuleName.substring(pathWithoutModuleName.indexOf(projectBaseDirPath) + projectBaseDirPath.length());
        }

        if (pathWithoutModuleName.startsWith(String.valueOf(PATH_SEPARATOR_CHAR))) {
            pathWithoutModuleName = pathWithoutModuleName.substring(1);
        }

        if (pathWithoutModuleName.lastIndexOf(PATH_SEPARATOR_CHAR) > -1) {
            this.displayFilename = pathWithoutModuleName.substring(pathWithoutModuleName.lastIndexOf(PATH_SEPARATOR_CHAR) + 1);
            this.displayPath = pathWithoutModuleName.substring(0, pathWithoutModuleName.lastIndexOf(PATH_SEPARATOR_CHAR));
        } else {
            this.displayFilename = pathWithoutModuleName;
            this.displayPath = "";
        }
    }

    public Module getModule() {
        return this.module;
    }

    public boolean isAnnotationBasedOnly() {
        return this.annotationBasedOnly;
    }

    public VirtualFile getFile() {
        return this.file;
    }

    public Collection<String> getClassNames() {
        return new ArrayList<>(this.classNames);
    }

    public String getDisplayFilename() {
        return this.displayFilename;
    }

    public String getDisplayPath() {
        return this.displayPath;
    }

    public Collection<EnhancerItem> toEnhancerItems() {
        final Collection<EnhancerItem> enhancerItems =
                new ArrayList<>(this.classNames.size() + (this.annotationBasedOnly ? 0 : 1));
        if (!this.annotationBasedOnly) {
            enhancerItems.add(new EnhancerItem(this, this.file));
        }
        for (final VirtualFile classFile : this.classFiles) {
            if (classFile != null) {
                enhancerItems.add(new EnhancerItem(this, classFile));
            }
        }
        return enhancerItems;
    }

}
