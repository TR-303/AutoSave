package com.github.tr303.autosave;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ViewVersionsAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        new AutoSaveWindow(e.getProject()).show();
    }
}
