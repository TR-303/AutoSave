package com.github.tr303.autosave;

import com.intellij.openapi.components.ApplicationComponent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

public class AutoSaveScheduler implements ApplicationComponent {
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false; // 状态标识符，表示任务是否正在运行

    @Override
    public void initComponent() {
        System.out.println("## construct.");
        startScheduler(); // 在组件初始化时启动任务
    }

    // 启动定时任务的方法
    public void startScheduler() {
        if (isRunning) {
            System.out.println("Scheduler is already running.");
            return;
        }

        scheduler = Executors.newScheduledThreadPool(1);

        // 每五秒调用一次指定函数
        scheduler.scheduleAtFixedRate(this::myScheduledFunction, 0, 5, TimeUnit.SECONDS);
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
    private void myScheduledFunction() {
        System.out.println("Executing scheduled function...");
        // 在这里实现你的函数逻辑
    }

    @Override
    public void disposeComponent() {
        // 组件被销毁时停止调度器
        stopScheduler();
    }
}
