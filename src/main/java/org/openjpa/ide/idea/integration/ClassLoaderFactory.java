package org.openjpa.ide.idea.integration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VirtualFile;

import com.intellij.util.lang.UrlClassLoader;
import org.openjpa.ide.idea.State;

/**
 * Factory for creating ClassLoaders restricted to each provided module's dependency scope.
 */
public abstract class ClassLoaderFactory {

    private ClassLoaderFactory() {
        // prohibit instantiation
    }

    /**
     * Creates a new {@link ClassLoader} that includes only the dependencies
     * and output dirs in the current module's compile context (includes module dependencies
     * and external jar dependencies).
     *
     * @param compileContext .
     * @param module         .
     * @param proxyClass     the class of the proxy instantiating a ClassLoader
     * @return .
     * @throws java.io.IOException .
     */
    @SuppressWarnings("deprecation") // want to stay backwards compatible at any cost
    public static ClassLoader newClassLoader(final CompileContext compileContext, final Module module, final Class<?> proxyClass) throws IOException {

        // get urls from actual class loader to be able to instantiate executors
        final UrlClassLoader loader = (UrlClassLoader) (proxyClass == null ? ClassLoaderFactory.class.getClassLoader() : proxyClass.getClassLoader());
        final Collection<URL> urls = new LinkedList<>(loader.getUrls());

        final var moduleOutputDirectory = compileContext.getModuleOutputDirectory(module);
        if (moduleOutputDirectory != null) {
            urls.add(toUrl(moduleOutputDirectory));
        }

        final var moduleOutputDirectoryForTests = compileContext.getModuleOutputDirectoryForTests(module);
        if (moduleOutputDirectoryForTests != null && State.getInstance(compileContext.getProject()).isIncludeTestClasses()){
            urls.add(toUrl(moduleOutputDirectoryForTests));
        }

        final List<VirtualFile> jars = new ArrayList<>();

        ModuleRootManager.getInstance(module).orderEntries().forEachLibrary(library -> {
            VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
            jars.addAll(Arrays.asList(files));
            return true;
        });

        for (final VirtualFile vf : jars) {
            final File f = new File(vf.getPath().replace(".jar!",".jar"));
            final URI uri = f.toURI();
            final URL url = uri.toURL();
            urls.add(url);
        }

        return new URLClassLoader(urls.toArray(new URL[0]));
    }

    private static URL toUrl(VirtualFile vf) throws IOException{
        final File file = new File(vf.getPath());
        final File canonicalFile = file.getCanonicalFile();
        final URI uri = canonicalFile.toURI();
        return uri.toURL();
    }
}
