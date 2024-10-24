package com.github.lylanv.secdroid.inspections;

import com.github.lylanv.secdroid.events.ApplicationStartedEvent;
import com.github.lylanv.secdroid.events.ApplicationStoppedEvent;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import org.jetbrains.annotations.NotNull;

public class AndroidProjectExecutionListener implements ExecutionListener{
    @Override
    public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {

        System.out.println("Process started");
        // Optional: Handle process start event if needed
        EventBusManager.post(new ApplicationStartedEvent(true));
    }

    @Override
    public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, ProcessHandler handler, int exitCode) {
//        // Check if the terminated process is related to your application
//        if (env.getRunProfile().getName().contains(applicationId)) {
//        }
        EventBusManager.post(new ApplicationStoppedEvent(true));

    }

}