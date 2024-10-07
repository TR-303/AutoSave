package com.github.tr303.autosave;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class AutoSaveStartUpActivity implements ProjectActivity {
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 启动自动保存调度器
        AutoSaveScheduler autoSaveScheduler = ApplicationManager.getApplication().getService(AutoSaveScheduler.class);
        if (autoSaveScheduler != null) {
            autoSaveScheduler.startScheduler();
        }

        String projectPath = project.getBasePath();
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                VirtualFile autosaveDir = VfsUtil.createDirectoryIfMissing(projectPath + "/.autosave");
                if (autosaveDir != null) {
                    String command = "attrib +h " + autosaveDir.getPath();
                    ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
                    processBuilder.start();

                    VfsUtil.createDirectoryIfMissing(autosaveDir, "objects");
                    VirtualFile versionsFile = autosaveDir.findChild("VERSIONS");
                    if (versionsFile == null) autosaveDir.createChildData(this, "VERSIONS");
                    VirtualFile referencesFile = autosaveDir.findChild("REFERENCES");
                    if (referencesFile == null) autosaveDir.createChildData(this, "REFERENCES");

                    FileTypeManager fileTypeManager = FileTypeManager.getInstance();
                    String ignoredFilesList = fileTypeManager.getIgnoredFilesList();
                    if (!ignoredFilesList.contains(".autosave")) {
                        String newIgnoredFilesList = ignoredFilesList.isEmpty() ? ".autosave" : ignoredFilesList + ";" + ".autosave";

                        ApplicationManager.getApplication().runWriteAction(() -> {
                            fileTypeManager.setIgnoredFilesList(newIgnoredFilesList);
                        });
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return Unit.INSTANCE;
    }
}