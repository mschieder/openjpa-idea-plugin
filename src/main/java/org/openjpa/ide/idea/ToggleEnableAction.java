package org.openjpa.ide.idea;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Action to be shown in and triggered by IDEA's 'Build' dialogue.<br/>
 * Build->OpenJpa Enhancer
 */
public class ToggleEnableAction extends ToggleAction {

    @Override
    public boolean isSelected(final @NotNull AnActionEvent anActionEvent) {
        var state = State.getInstance(getProject(anActionEvent));
        return state.isEnhancerEnabled();
    }

    @Override
    public void setSelected(final @NotNull AnActionEvent anActionEvent, final boolean b) {
        State.getInstance(getProject(anActionEvent)).setEnhancerEnabled(b);
    }

    private static Project getProject(final AnActionEvent anActionEvent) {
        return CommonDataKeys.PROJECT.getData(anActionEvent.getDataContext());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return  ActionUpdateThread.BGT;
    }
}
