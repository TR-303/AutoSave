package com.github.tr303.autosave;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

public class AutoSaveScheduler implements AutoCloseable {
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false; // 状态标识符，表示任务是否正在运行
    private boolean hasChanged = false; // 上次保存后是否有新的更改
    private Instant lastEditTime = Instant.now(); // 上次编辑的时间
    private Instant lastSaveTime = Instant.now(); // 上次保存的时间

    // 构造函数，在服务初始化时启动调度任务
    public AutoSaveScheduler() {
        startScheduler(); // 在服务初始化时启动任务
        addDocumentListeners(); // 添加文档监听器
    }

    // 启动定时任务的方法
    public void startScheduler() {
        if (isRunning) {
            System.out.println("Scheduler is already running.");
            return;
        }

        scheduler = Executors.newScheduledThreadPool(1);

        // 每秒检查一次保存条件（10秒未编辑 或 5分钟内未保存）
        scheduler.scheduleAtFixedRate(this::checkSaveConditions, 0, 1, TimeUnit.SECONDS);
        isRunning = true; // 更新状态为运行中
        System.out.println("Scheduler started.");
    }

    // 手动停止定时任务的方法
    public void stopScheduler() {
        if (!isRunning) {
            System.out.println("Scheduler is not running.");
            return;
        }

        if (scheduler != null) {
            scheduler.shutdown();
            isRunning = false; // 更新状态为已停止
            System.out.println("Scheduler stopped.");
        }
    }

    // 添加文档监听器，遍历所有项目中的文档
    private void addDocumentListeners() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : openProjects) {
            VirtualFile projectBaseDir = project.getBaseDir();
            if (projectBaseDir != null) {
                ApplicationManager.getApplication().runReadAction(() -> {
                    addListenersToDirectory(projectBaseDir); // 确保在读取操作中调用
                });
            }
        }
    }

    // 遍历目录中的所有文件并为它们添加 DocumentListener
    private void addListenersToDirectory(VirtualFile directory) {
        for (VirtualFile file : directory.getChildren()) {
            if (file.isDirectory()) {
                addListenersToDirectory(file); // 递归遍历子目录
            } else {
                Document document = ApplicationManager.getApplication().runReadAction((Computable<Document>) () -> {
                    return FileDocumentManager.getInstance().getDocument(file);
                });
                if (document != null) {
                    document.addDocumentListener(new DocumentListener() {
                        @Override
                        public void documentChanged(@NotNull DocumentEvent event) {
                            lastEditTime = Instant.now(); // 更新上次编辑时间
                            hasChanged = true;
                        }
                    });
                }
            }
        }
    }

    // 定时检查是否满足保存条件
    private void checkSaveConditions() {
        Duration sinceLastEdit = Duration.between(lastEditTime, Instant.now());
        Duration sinceLastSave = Duration.between(lastSaveTime, Instant.now());

        // 近期未编辑
        if (hasChanged && sinceLastEdit.getSeconds() >= 3) {
            autoSave("Auto Save: User Stopped Editing");
        }

        // 1分钟内无任何保存
        if (hasChanged && sinceLastSave.getSeconds() >= 60) {
            autoSave("Auto Save: 1 min since last version");
        }
    }

    // 自动保存函数
    private void autoSave(String saveReason) {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length == 0) {
            System.out.println("No open projects.");
            return;
        }
        System.out.println("########### " + saveReason + " ###########");

        // 确保保存操作在 Event Dispatch Thread (EDT) 中同步执行
        ApplicationManager.getApplication().invokeAndWait(() -> {
            FileDocumentManager.getInstance().saveAllDocuments();
        });

        for (Project project : openProjects) {
            if (project == null) continue; // 安全检查

            Notification notification = new Notification("AutoSaveNotifications", "Saving current version", "Please wait...", NotificationType.INFORMATION);
            Notifications.Bus.notify(notification, project);

            Boolean result = new AutoSaveFunctional(project).saveCurrentProjectAsVersion(saveReason);

            if (result != null && result) {
                notification.setContent("Succeed！");
                lastSaveTime = Instant.now(); // 更新上次保存时间
                hasChanged = false;
            } else {
                notification.setContent("No change detected！There is nothing to save");
                hasChanged = false;
            }

            Notifications.Bus.notify(notification, project);
        }
    }

    @Override
    public void close() {
        stopScheduler(); // 调用关闭方法时停止调度任务
    }
}
