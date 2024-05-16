package org.openjpa.ide.idea;

import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.openjpa.ide.idea.integration.EnhancerSupport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Component registering the enhancer computable and handling the plugin's state
 * (Interacting with configuration GUI by converting between the different state models)
 */
public class ProjectComponent extends AbstractProjectComponent {

    public static final ExtensionPointName<EnhancerSupport> EP_NAME = EnhancerSupport.EXTENSION_POINT_NAME;


    //
    // Members
    //

    /**
     * Current project
     */
    private final Project project;

    /**
     * Persistent configuration
     */
   private final State state;

    /**
     * Enhancer instance (created on first build run)
     */
    private Computable dNEComputable = null;

    //
    // Constructor
    //

    public ProjectComponent(final Project p) {
        super(p);
        this.project = p;
        var ps = PersistentState.getInstance(p);
        this.state =new State(ps);
        final EnhancerSupportRegistry enhancerSupportRegistry = this.state.getEnhancerSupportRegistry();
        enhancerSupportRegistry.registerEnhancerSupport(EnhancerSupportRegistryDefault.DEFAULT_ENHANCER_SUPPORT);
        final EnhancerSupport[] enhancerSupports =
                (EnhancerSupport[]) Extensions.getExtensions(EnhancerSupport.EXTENSION_POINT_NAME);

        for (final EnhancerSupport enhancerSupport : enhancerSupports) {
            enhancerSupportRegistry.registerEnhancerSupport(enhancerSupport);
        }
    }

    public State getState() {
        return state;
    }

    //
    // ProjectComponent Interface implementation
    //

    @Override
    public void projectOpened() {
        super.projectOpened();
        this.dNEComputable = new Computable(this.project, ProjectComponent.this.state);
        // run enhancer after compilation
        final CompilerManager compilerManager = CompilerManager.getInstance(this.project);
        compilerManager.addCompiler(this.dNEComputable);
    }


    @SuppressWarnings("RefusedBequest")
    @NonNls
    @NotNull
    @Override
    public String getComponentName() {
        return "OpenJpa Enhancer";
    }


    Map<Module, List<VirtualMetadataFile>> getAnnotatedClassFiles() {
       return this.dNEComputable == null ? new LinkedHashMap<Module, List<VirtualMetadataFile>>()
                        : this.dNEComputable.getAnnotatedClassFiles(null);
    }

    Map<Module, List<VirtualMetadataFile>> getMetadataFiles() {
        return this.dNEComputable == null ? new LinkedHashMap<Module, List<VirtualMetadataFile>>()
                        : this.dNEComputable.getMetadataFiles(null);
    }

    boolean isEnhancerInitialized(){
        return this.dNEComputable != null;
    }

}
