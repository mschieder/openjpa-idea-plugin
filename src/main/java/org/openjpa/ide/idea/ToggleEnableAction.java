package org.openjpa.ide.idea;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;

/**
 * Action to be shown in and triggered by IDEA's 'Build' dialogue.<br/>
 * Build->OpenJpa Enhancer
 */
public class ToggleEnableAction extends ToggleAction {

    @Override
    public boolean isSelected(final AnActionEvent anActionEvent) {
        var state = State.getInstance(anActionEvent.getProject());
        return state.isEnhancerEnabled();
    }

    @Override
    public void setSelected(final AnActionEvent anActionEvent, final boolean b) {
        State.getInstance(anActionEvent.getProject()).setEnhancerEnabled(b);
    }

    private static Project getProject(final AnActionEvent anActionEvent) {
        return PlatformDataKeys.PROJECT.getData(anActionEvent.getDataContext());
    }

}
