package com.github.tr303.autosave;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class SaveManuallyAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        ApplicationManager.getApplication().invokeAndWait(() -> {
            FileDocumentManager.getInstance().saveAllDocuments();
        });

        Notification notification = new Notification("AutoSaveNotifications", "Saving current version", "Please wait...", NotificationType.INFORMATION);
        Notifications.Bus.notify(notification, project);

        Boolean result = new AutoSaveFunctional(project).saveCurrentProjectAsVersion("User Quick Saved");

        if (result != null && result) {
            notification.setContent("Succeed！");
        } else {
            notification.setContent("Failed！There is nothing to save");
        }

        Notifications.Bus.notify(notification, project);
    }
}
