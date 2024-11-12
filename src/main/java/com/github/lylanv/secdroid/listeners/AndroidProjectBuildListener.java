package com.github.lylanv.secdroid.listeners;

import com.github.lylanv.secdroid.events.BuildSuccessEvent;
import com.android.tools.idea.projectsystem.ProjectSystemBuildManager;
import com.android.tools.idea.projectsystem.ProjectSystemBuildManager.BuildListener;
import com.github.lylanv.secdroid.inspections.EventBusManager;

public class AndroidProjectBuildListener implements BuildListener {

    @Override
    public void buildCompleted(final ProjectSystemBuildManager.BuildResult buildStatus) {
        if (buildStatus.getStatus() == ProjectSystemBuildManager.BuildStatus.SUCCESS){
            System.out.println("Build completed successfully");
            EventBusManager.post(new BuildSuccessEvent(true));
        }
    }

}