package com.github.tr303.autosave;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class SaveManuallyAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
//        new AutoSaveFunctional(e.getProject()).saveCurrentProjectAsVersion("User Quick Saved");
    }
}
