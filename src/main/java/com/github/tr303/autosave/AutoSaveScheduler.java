package com.github.tr303.autosave;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

public class AutoSaveScheduler implements Disposable {
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false; // 状态标识符，表示任务是否正在运行
    private boolean hasChanged = false; // 上次保存后是否有新的更改
    private Instant lastEditTime = Instant.now(); // 上次编辑的时间
    private Instant lastSaveTime = Instant.now(); // 上次保存的时间

    // 构造函数，在服务初始化时启动调度任务
    public AutoSaveScheduler() {
        startScheduler(); // 在服务初始化时启动任务
        addDocumentListeners(); // 添加文档监听器
        addProjectCloseListener(); // 添加项目关闭监听器
    }

    // 启动定时任务的方法
    public void startScheduler() {
        if (isRunning) {
            System.out.println("Scheduler is already running.");
            return;
        }

        scheduler = Executors.newScheduledThreadPool(1);

        // 每秒检查一次保存条件（10秒未编辑 或 1分钟内未保存）
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
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                lastEditTime = Instant.now(); // 更新上次编辑时间
                hasChanged = true;
            }
        });
    }

    // 订阅项目关闭事件
    private void addProjectCloseListener() {
        MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();

        // 订阅 ProjectManagerListener 的 TOPIC
        messageBus.connect(this).subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
            @Override
            public void projectClosing(@NotNull Project project) {
                // 当项目关闭时自动保存项目
                autoSaveOnProjectClose(project);
            }
        });
    }

    // 定时检查是否满足保存条件
    private void checkSaveConditions() {
        Duration sinceLastEdit = Duration.between(lastEditTime, Instant.now());
        Duration sinceLastSave = Duration.between(lastSaveTime, Instant.now());

        // 近期未编辑
        if (hasChanged && sinceLastEdit.getSeconds() >= 10) {
            autoSave("Auto Save: User Stopped Editing");
        }

        // 1分钟内无任何保存
        if (hasChanged && sinceLastSave.getSeconds() >= 60) {
            autoSave("Auto Save: 1 min since last version");
        }
    }

    // 自动保存函数
    private void autoSave(String saveReason) {
        System.out.println("########### " + saveReason + " ###########");
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length == 0) {
            System.out.println("No open projects.");
            return;
        }

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

    // 项目关闭时自动保存项目版本
    private void autoSaveOnProjectClose(Project project) {
        System.out.println("########### Auto Save on Project Close ###########");

        // 确保保存操作在 Event Dispatch Thread (EDT) 中同步执行
        ApplicationManager.getApplication().invokeAndWait(() -> {
            FileDocumentManager.getInstance().saveAllDocuments();
        });

        if (project != null) {
            Notification notification = new Notification("AutoSaveNotifications", "Saving current version", "Please wait...", NotificationType.INFORMATION);
            Notifications.Bus.notify(notification, project);

            Boolean result = new AutoSaveFunctional(project).saveCurrentProjectAsVersion("Auto Save on Project Close");

            if (result != null && result) {
                notification.setContent("Succeed！");
            } else {
                notification.setContent("No change detected！There is nothing to save");
            }

            Notifications.Bus.notify(notification, project);
        }
    }

    @Override
    public void dispose() {
        stopScheduler(); // 调用关闭方法时停止调度任务
    }
}
