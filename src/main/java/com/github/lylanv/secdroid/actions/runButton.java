package com.github.lylanv.secdroid.actions;

//import com.android.tools.lint.detector.api.Project;
//import com.android.ide.common.process.ProcessOutput;
import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker;
import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker.Request;
import com.android.tools.idea.gradle.project.sync.GradleSyncListener;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.ModuleManager;
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
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.lylanv.secdroid.commands.AdbUtils.*;
import static com.github.lylanv.secdroid.commands.EmulatorUtils.getAvailableAVDs;

public class runButton extends AnAction {
    Project project;
    //Boolean buildSuccess;
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

        // Refresh Virtual File System (Detects new/modified files)
        VirtualFileManager.getInstance().syncRefresh();
//
//        // Refresh project structure (Detects added/removed modules)
//        ModuleManager.getInstance(project).getModules(); // Just accessing forces refresh


//        GradleSyncInvoker.Request request = GradleSyncInvoker.Request.testRequest();
//        GradleSyncInvoker.getInstance().requestProjectSync(project, request, null);

//
        // Force Gradle Sync before building to capture changes
        GradleSyncInvoker.getInstance().requestProjectSync(project,
                GradleSyncInvoker.Request.testRequest(),
                new GradleSyncListener() {
                    @Override
                    public void syncSucceeded(@NotNull Project project) {
                        System.out.println("Gradle Sync completed successfully!");
                        buildApplication(project); // Start build after successful sync
                    }

                    @Override
                    public void syncFailed(@NotNull Project project, @NotNull String errorMessage) {
                        System.out.println("Gradle Sync failed: " + errorMessage);
                    }
                }
        );


        //buildSuccess = buildApplication(project);
        //buildApplication(project);

    }


    private void buildApplication(Project project) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Building Application",false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setText("Building APK...");
                progressIndicator.setIndeterminate(true);

                // Build the APK
                AtomicBoolean buildSuccessful = new AtomicBoolean(false);
                AtomicBoolean buildTestSuccessful = new AtomicBoolean(false);
                String apkPath = runBuildApk(project,buildSuccessful);
                String testPath = runBuildTest(project,buildTestSuccessful);


                // Run UI operation on Event Dispatch Thread (EDT)
                ApplicationManager.getApplication().invokeLater(() -> {
                    //if (apkPath != null && buildSuccessful.get()) {
                    if (apkPath == null || !buildSuccessful.get()) {
                        Messages.showErrorDialog(project, "Failed to build the APK", "Build Failed");
                    } else {

                        if (testPath == null || !buildTestSuccessful.get()) {
                            Messages.showErrorDialog(project, "Failed to build the Test APK", "Test Build Failed");
                        }else {
                            Messages.showInfoMessage(project, "Build Successful! APK path: " + apkPath, "Build Completed");
                            Messages.showInfoMessage(project, "Test Build Successful! Test APK path: " + testPath, "Test Build Completed");
                            runApplication(apkPath);
                        }
                    }
                });
            }
        });

    }

    private String runBuildApk(Project project, AtomicBoolean success) {
        try {
            // To build the project run the gradle assembleDebug task first
            String gradleProjectPath = project.getBasePath();

            if (gradleProjectPath == null) {return null;}

            CompletableFuture<String> future = new CompletableFuture<>();

            String gradleWrapper = null;
            if (os.contains("windows")) {
                gradleWrapper = gradleProjectPath + "\\gradlew.bat"; //"gradlew.bat"
            }else if (os.contains("linux") || os.contains("mac")) {
                gradleWrapper = gradleProjectPath + "/gradlew";
            }

            System.out.println("[runButton -> actionPerformed -> buildApplication -> runBuildApk $ Building APK...");
            String gradleTask = "assembleDebug";

            try {
                ProcessBuilder pb = new ProcessBuilder(gradleWrapper, gradleTask);
                pb.directory(new File(project.getBasePath()));
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;

                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

                // Check exit code
                int exitCode = process.waitFor();
                success.set(exitCode == 0);

                reader.close();

                if (success.get()) {
                    String apkPath = gradleProjectPath +
                            "/app/build/outputs/apk/debug/app-debug.apk";

                    File apkFile = new File(apkPath);
                    if (apkFile.exists()) {
                        System.out.println("runButton -> actionPerformed -> buildApplication -> runBuildApk $ Built is completed and APK is generated successfully!");
                        System.out.println("runButton -> actionPerformed -> buildApplication -> runBuildApk $ APK path is <" + apkPath + ">");
                        future.complete(apkPath);
                    }else {
                        future.complete(null);
                        System.out.println("runButton -> actionPerformed -> buildApplication -> runBuildApk $ Built failed and generating APK failed!");
                    }
                } else {
                    future.complete(null);
                }

            } catch (Exception e) {
                e.printStackTrace();
                success.set(false);
                future.complete(null);
            }


            //return success.get() ? "Success" : "Failure";
            return future.get(5, TimeUnit.MINUTES);

        } catch (Exception e) {
            e.printStackTrace();
            success.set(false);
            return null;
        }
    }

    private String runBuildTest(final Project project, AtomicBoolean success) {
        try {
            // To build the project run the gradle assembleDebug task first
            String gradleProjectPath = project.getBasePath();

            if (gradleProjectPath == null) {return null;}

            CompletableFuture<String> future = new CompletableFuture<>();

            String gradleWrapper = null;
            if (os.contains("windows")) {
                gradleWrapper = gradleProjectPath + "\\gradlew.bat"; //"gradlew.bat"
            }else if (os.contains("linux") || os.contains("mac")) {
                gradleWrapper = gradleProjectPath + "/gradlew";
            }

            System.out.println("[runButton -> actionPerformed -> buildApplication -> runBuildTest$ Building Test APK...");
            String gradleTask = "assembleAndroidTest";

            try {
                ProcessBuilder pb = new ProcessBuilder(gradleWrapper, gradleTask);
                pb.directory(new File(project.getBasePath()));
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;

                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

                // Check exit code
                int exitCode = process.waitFor();
                success.set(exitCode == 0);

                reader.close();

                if (success.get()) {
                    String testApkPath = gradleProjectPath +
                            "/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk";

                    File apkFile = new File(testApkPath);
                    if (apkFile.exists()) {
                        System.out.println("runButton -> actionPerformed -> buildApplication -> runBuildTest $ Built is completed and Test APK is generated successfully!");
                        System.out.println("runButton -> actionPerformed -> buildApplication -> runBuildTest $ Test APK path is <" + testApkPath + ">");
                        future.complete(testApkPath);
                    }else {
                        future.complete(null);
                        System.out.println("runButton -> actionPerformed -> buildApplication -> runBuildTest $ Built failed and generating Test APK failed!");
                    }
                } else {
                    future.complete(null);
                }

            } catch (Exception e) {
                e.printStackTrace();
                success.set(false);
                future.complete(null);
            }


            //return success.get() ? "Success" : "Failure";
            return future.get(5, TimeUnit.MINUTES);

        } catch (Exception e) {
            e.printStackTrace();
            success.set(false);
            return null;
        }
    }

    private void runApplication(String apkPath) {
        int numberOfAvailableEmulators = 0; // holds number of the emulators that already are running
        int numberOfNeededEmulators = 0; // holds number of the emulators that should be started
        int numberOfUserRequestedEmulators = getUserRequestedNumberOfEmulators(); // holds number of the emulators that user requested
        System.out.println("BCProf -> runButton -> runApplication$ Number of user requested emulators " + numberOfUserRequestedEmulators);

        List<String> runningEmulators = new ArrayList<>(); // holds list of the already running emulators' names (in the form of emulator-....)
        List<String> availableEmulators = new ArrayList<>(); // holds list of the emulators' names that already running and they are free
        List<String> availableAVDs = new ArrayList<>(); // holds list of the emulators' avds names that already running and they are free
        List<String> newlyStartedEmulators = new ArrayList<>(); // holds list of the emulators' names that are just started
        List<String> busyEmulators = new ArrayList<>(); //holds busy running emulators' names
        List<String> busyAVDs = new ArrayList<>(); //holds busy running emulators' avds names


        // getting already running emulators
        runningEmulators = getRunningEmulators(); //get the already running emulators (in the form of emulator-....)
        if (runningEmulators == null) {
            System.out.println("There isn't any running emulators");
            numberOfNeededEmulators = numberOfUserRequestedEmulators; // if there is not any running emulator we should start the amount that user is requested
            System.out.println("Number of needed emulators to start " + numberOfNeededEmulators);
        }else{

            System.out.println("There are " + runningEmulators.size() + " running emulators");
            for (String runningEmulator : runningEmulators) {
                System.out.println("Running emulator: " + runningEmulator);
                Boolean emulatorRunningAnotherApp = isEmulatorRunningAnotherApp(runningEmulator, os); // check if emulator is running another app

                // if the emulator is not running another app we can run the app on that emulator
                if (!emulatorRunningAnotherApp) {
                    String avdName = getEmulatorName(runningEmulator,os); // gets the running emulator's AVD name

                    if (avdName != null) {
                        availableAVDs.add(avdName);

                        System.out.println("Available AVD name: " + avdName);

                        numberOfAvailableEmulators++;
                        availableEmulators.add(runningEmulator);
                        System.out.println("The emulator " + runningEmulator + " is available to run the application.");
                    }

                }else {
                    String busyAVD = getEmulatorName(runningEmulator,os);
                    if (busyAVD != null) {
                        busyAVDs.add(busyAVD);

                        busyEmulators.add(runningEmulator);
                        System.out.println("Busy AVD name: " + busyAVD);
                    }
                }

            }

            if (numberOfAvailableEmulators >= numberOfUserRequestedEmulators) {
                numberOfNeededEmulators = 0;
                System.out.println(numberOfNeededEmulators + " emulators are needed to start.");
                //TODO: Run the app on subset of the available emulators

            }else {
                numberOfNeededEmulators = numberOfUserRequestedEmulators -  numberOfAvailableEmulators;
                System.out.println(numberOfNeededEmulators + " emulators are needed to start.");
                if ((numberOfNeededEmulators + numberOfAvailableEmulators) > 64){
                    Messages.showErrorDialog("Failed to run the application on emulators. Number of requested emulators is not feasible according to the number of already running emulators.", "Failed Running The Application");
                    numberOfNeededEmulators = 0;
                }
            }

            if (numberOfNeededEmulators > 0){
                newlyStartedEmulators = startEmulators(numberOfNeededEmulators, runningEmulators, availableEmulators,availableAVDs, busyEmulators, busyAVDs);
            }

        }


    }


    private int getUserRequestedNumberOfEmulators() {
        int numberOfEmulators = -1;

        while (numberOfEmulators < 1) {

            String numberOfDesiredEmulators = Messages.showInputDialog(
                    "Enter the number of emulators to run the application on:",
                    "Number of Emulators",
                    Messages.getQuestionIcon()
            );
            try {
                numberOfEmulators = Integer.parseInt(numberOfDesiredEmulators);

                if (numberOfEmulators < 1) {
                    Messages.showErrorDialog("Invalid number. The number of emulators should be more than zero.", "Number of Emulators Error");
                }
                if (numberOfEmulators > 64) {
                    Messages.showErrorDialog("Invalid number. The maximum number of emulators is 64.", "Number of Emulators Error");
                    numberOfEmulators = -1;
                }
            } catch (NumberFormatException e) {
                Messages.showErrorDialog("Invalid number. Please enter a valid integer.", "Number of Emulators Error");
            }

        }

        return numberOfEmulators;
    }

    private List<String> startEmulators(int numberOfEmulators, List<String> runningEmulators, List<String> availableEmulators, List<String> availableAVDs, List<String> busyEmulators, List<String> busyAVDs) {
        List<String> newEmulators = new ArrayList<>();


//        List<String> availableAVDs = new ArrayList<>();

//        availableAVDs = getAvailableAVDs();
//
//        for (String avd : availableAVDs) {
//            boolean isRunning = false;
//            for (String runningEmulator : runningEmulators) {
//                if (runningEmulator.contains(avd)) {
//                    isRunning = true;
//                    break;
//                }
//            }
//
//            if (!isRunning) {
//                newEmulators.add(avd);
//            }
//        }
//
//
//        return newEmulators;
        return newEmulators;
    }

}
