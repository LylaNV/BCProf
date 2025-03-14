package com.github.lylanv.secdroid.actions;

//import com.android.tools.lint.detector.api.Project;
//import com.android.ide.common.process.ProcessOutput;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
//import org.jetbrains.plugins.gradle.service.task.GradleTaskManager;
//import org.jetbrains.plugins.gradle.service.task.GradleTaskResultListener;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.android.tools.r8.internal.Sy;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class runButton extends AnAction {
    Project project;
    Boolean buildSuccess;
    String os;

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        System.out.println("[runButton -> actionPerformed$ BCProf Run button is clicked");

        project = event.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            System.out.println("[runButton -> actionPerformed$ FATAL ERROR: Project cannot be detected!");
            return;
        }

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            os = "windows";
        } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            os = "mac";
        }else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            os = "linux";
        }else {
            System.out.println("BCProf -> runButton -> actionPerformed$ FATAL ERROR: Operating System is not supported");
            Messages.showErrorDialog(project, "Your Operating System is not supported by BCProf!", "Build Stopped");
            return;
        }

        //buildSuccess = buildApplication(project);
        buildApplication(project);

    }


    private void buildApplication(Project project) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Building Application",false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setText("Building APK...");
                progressIndicator.setIndeterminate(true);

                // Build the APK
                AtomicBoolean buildSuccessful = new AtomicBoolean(false);
                String apkPath = runBuildApk(project,buildSuccessful);


                // Run UI operation on Event Dispatch Thread (EDT)
                ApplicationManager.getApplication().invokeLater(() -> {
                    //if (apkPath != null && buildSuccessful.get()) {
                    if (apkPath != null) {
                        Messages.showInfoMessage(project, "Build Successful! APK path: " + apkPath, "Build Completed");
                    } else {
                        Messages.showErrorDialog(project, "Build Failed!", "Build Error");
                    }
                });
            }
        });

        //return buildSuccess;
    }

    private String runBuildApk(Project project, AtomicBoolean success) {
        try {
            // To build the project run the gradle assembleDebug task first
            String gradleProjectPath = project.getBasePath();

            String gradleWrapper = null;
            if (os.contains("windows")) {
                gradleWrapper = gradleProjectPath + "\\gradlew.bat"; //"gradlew.bat"
            }else if (os.contains("linux") || os.contains("mac")) {
                gradleWrapper = gradleProjectPath + "/gradlew";
            }

            System.out.println("[runButton -> actionPerformed -> buildApplication -> buildApk $ Building APK...");
            String gradleFirstTask = "assembleDebug";

            System.out.println("[runButton -> actionPerformed -> buildApplication -> buildApk $ apk path = " + gradleProjectPath);

            try {
                //ProcessBuilder pb = new ProcessBuilder(gradleProjectPath, gradleFirstTask, gradleSecondTask, gradleThirdTask, gradleFourthTask);
                ProcessBuilder pb = new ProcessBuilder(gradleWrapper, gradleFirstTask);
                pb.directory(new File(project.getBasePath()));
                pb.redirectErrorStream(true);
                Process process = pb.start();


                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                boolean buildSuccess = false;

                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    if (line.contains("BUILD SUCCESSFUL")) {
                        buildSuccess = true;
                    }
                }

                // Check exit code
                int exitCode = process.waitFor();
                if (exitCode == 0 && buildSuccess) {
                    System.out.println("Build successful!");
                    success.set(true);
                } else {
                    System.out.println("Build failed.");
                    success.set(false);
                }

            } catch (Exception e) {
                e.printStackTrace();
                success.set(false);
            }

            return success.get() ? "Success" : "Failure";

        } catch (Exception e) {
            e.printStackTrace();
            success.set(false);
 //         future.complete(null);
        }

        return null;
    }

}
