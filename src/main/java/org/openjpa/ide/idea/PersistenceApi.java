package org.openjpa.ide.idea;

import java.util.Arrays;

import static org.openjpa.ide.idea.PersistenceApiConstants.ANNOTATION_JAKARTA_PERSISTENCE_EMBEDDABLE;
import static org.openjpa.ide.idea.PersistenceApiConstants.ANNOTATION_JAKARTA_PERSISTENCE_ENTITY;
import static org.openjpa.ide.idea.PersistenceApiConstants.ANNOTATION_JAKARTA_PERSISTENCE_MAPPED_SUPERCLASS;
import static org.openjpa.ide.idea.PersistenceApiConstants.ANNOTATION_JAVA_PERSISTENCE_EMBEDDABLE;
import static org.openjpa.ide.idea.PersistenceApiConstants.ANNOTATION_JAVA_PERSISTENCE_ENTITY;
import static org.openjpa.ide.idea.PersistenceApiConstants.ANNOTATION_JAVA_PERSISTENCE_MAPPED_SUPERCLASS;

/**
 * Enum defining supported persistence api's, including information (fq class names) about
 * corresponding annotations.
 */
public enum PersistenceApi {

    HIBERNATE(),

    JPA(ANNOTATION_JAVA_PERSISTENCE_ENTITY,
            ANNOTATION_JAVA_PERSISTENCE_MAPPED_SUPERCLASS,
            ANNOTATION_JAVA_PERSISTENCE_EMBEDDABLE,
            ANNOTATION_JAKARTA_PERSISTENCE_ENTITY,
            ANNOTATION_JAKARTA_PERSISTENCE_MAPPED_SUPERCLASS,
            ANNOTATION_JAKARTA_PERSISTENCE_EMBEDDABLE
    );

    private final String[] annotationClassNames;

    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
        // no outside reference
    PersistenceApi(final String... annotationClassNames) {
        this.annotationClassNames = annotationClassNames;
    }

    public String[] getAnnotationClassNames() {
        return Arrays.copyOf(this.annotationClassNames, this.annotationClassNames.length);
    }

}
