package com.github.tr303.autosave;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

public class AutoSaveScheduler implements AutoCloseable {
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false; // 状态标识符，表示任务是否正在运行

    // 构造函数，在服务初始化时启动调度任务
    public AutoSaveScheduler() {
        startScheduler(); // 在服务初始化时启动任务
    }

    // 启动定时任务的方法
    public void startScheduler() {
        if (isRunning) {
            System.out.println("Scheduler is already running.");
            return;
        }

        scheduler = Executors.newScheduledThreadPool(1);

        // 每 30 秒调用一次 autoSave() 函数
        scheduler.scheduleAtFixedRate(this::autoSave, 0, 30, TimeUnit.SECONDS);
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

    // 定时任务的执行函数
    private void autoSave() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length == 0) {
            System.out.println("No open projects.");
            return;
        }
        System.out.println("########### Auto Save ###########");

        // 确保保存操作在 Event Dispatch Thread (EDT) 中同步执行
        ApplicationManager.getApplication().invokeAndWait(() -> {
            FileDocumentManager.getInstance().saveAllDocuments();
        });

        for (Project project : openProjects) {
            if (project == null) continue;// 似乎不会发生？

            Notification notification = new Notification("AutoSaveNotifications", "Saving current version", "Please wait...", NotificationType.INFORMATION);
            Notifications.Bus.notify(notification, project);

            Boolean result = new AutoSaveFunctional(project).saveCurrentProjectAsVersion("30'' Auto Save ");

            if (result != null && result) {
                notification.setContent("Succeed！");
            } else {
                notification.setContent("No change detected！There is nothing to save");
            }

            Notifications.Bus.notify(notification, project);
        }
    }

    @Override
    public void close() {
        stopScheduler(); // 调用关闭方法时停止调度任务
    }
}
