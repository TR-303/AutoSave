package com.github.tr303.autosave;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFileFilter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

public class SaveVersionAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        System.out.println("########0");
        Project project = e.getProject();
        if (project != null) {
            System.out.println("########1");
            saveVersionSnapshot(project);
        }
    }

    private void saveVersionSnapshot(Project project) {
        System.out.println("########2");
        String projectPath = project.getBasePath();

        // 创建格式化器，精确到秒
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String timestamp = sdf.format(new Date());

        // 包含时间戳的版本文件夹路径
        String versionFolderPath = projectPath + "/.version_control";
        String versionFolderPath_with_time = versionFolderPath + "/" + timestamp;

        // 获取项目的虚拟文件系统根目录
        VirtualFile projectFile = project.getBaseDir();

        // 检查版本文件夹是否已存在
        AtomicReference<VirtualFile> versionFolder = new AtomicReference<>(project.getBaseDir().findChild(".version_control"));
        if (versionFolder.get() != null) {
            for (VirtualFile file : versionFolder.get().getChildren()) {
                if (file.getName().equals(timestamp)) {
                    // 如果已存在相同名字的文件夹，显示提示信息
                    Messages.showInfoMessage("当前版本已保存过了: " + versionFolderPath, "版本已存在");
                    return; // 退出方法
                }
            }
        }

        // 创建版本文件夹
//                versionFolder = VfsUtil.createDirectoryIfMissing(versionFolderPath);
        System.out.println("########3");
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                versionFolder.set(VfsUtil.createDirectoryIfMissing(versionFolderPath_with_time));

                System.out.println("########4");
                // 过滤器，排除隐藏文件夹
                VirtualFileFilter filter = file -> !file.getName().startsWith(".");

                // 复制项目文件到版本文件夹，排除隐藏文件夹
                VfsUtil.copyDirectory(this, projectFile, versionFolder.get(), filter);
                System.out.println("Version snapshot saved in: " + versionFolderPath);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to save version snapshot: " + e.getMessage());
            }
            System.out.println(versionFolderPath_with_time);
        });
    }
}
