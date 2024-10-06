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

import java.io.File;
import java.io.IOException;

public class AutoSaveStartUpActivity implements ProjectActivity {
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
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
                }

                VirtualFile gitignoreFile = VfsUtil.findFileByIoFile(new File(projectPath + "/.idea/.gitignore"), true);
                if (gitignoreFile != null) {
                    String content = VfsUtil.loadText(gitignoreFile);
                    if (!content.contains(".autosave")) {
                        String newContent = content + ".autosave\n";
                        gitignoreFile.setBinaryContent(newContent.getBytes());
                    }
                } else {
                    gitignoreFile = VfsUtil.createDirectoryIfMissing(projectPath + "/.idea").createChildData(this, ".gitignore");
                    gitignoreFile.setBinaryContent(".autosave\n".getBytes());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return Unit.INSTANCE;
    }
}
