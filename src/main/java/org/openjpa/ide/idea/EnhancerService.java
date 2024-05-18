package org.openjpa.ide.idea;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service(Service.Level.PROJECT)
public final class EnhancerService {

    private final Computable dNEComputable;

    EnhancerService(Project project){
        var state = State.getInstance(project);
        this.dNEComputable = new Computable(project, state);
    }

    static EnhancerService getInstance(Project project){
        return project.getService(EnhancerService.class);
    }

    public Computable getdNEComputable() {
        return dNEComputable;
    }

    Map<Module, List<VirtualMetadataFile>> getAnnotatedClassFiles() {
        return this.dNEComputable == null ? new LinkedHashMap<>()
                : this.dNEComputable.getAnnotatedClassFiles(null);
    }

    Map<Module, List<VirtualMetadataFile>> getMetadataFiles() {
        return this.dNEComputable == null ? new LinkedHashMap<>()
                : this.dNEComputable.getMetadataFiles(null);
    }

    boolean isEnhancerInitialized() {
        return this.dNEComputable != null;
    }
}
