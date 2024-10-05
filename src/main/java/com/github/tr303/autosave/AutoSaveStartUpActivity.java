package com.github.tr303.autosave;


import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class AutoSaveStartUpActivity implements ProjectActivity {
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        String projectPath = project.getBasePath();
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                VirtualFile autosaveDir = VfsUtil.createDirectoryIfMissing(projectPath + "/.autosave");
                VirtualFile objectsDir = VfsUtil.createDirectoryIfMissing(autosaveDir, "objects");
                if (autosaveDir != null) {
                    VirtualFile versionsFile = autosaveDir.findChild("VERSIONS");
                    if (versionsFile == null) autosaveDir.createChildData(this, "VERSIONS");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return Unit.INSTANCE;
    }
}
