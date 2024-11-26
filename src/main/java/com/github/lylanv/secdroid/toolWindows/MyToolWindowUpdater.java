package com.github.lylanv.secdroid.toolWindows;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;

/* We have this class to call the refreshToolWindow method in the LogcatAnalyzerToolWindowFactory
 the reason is that making us able to run and stop application multiple times and refresh the toolWindow
 without needing to go to another toolwindows and then come back to this one to be able to see the data
 * */
public class MyToolWindowUpdater {
    private static MyToolWindowUpdater instance;
    private final Project project;

    private MyToolWindowUpdater(Project project) {
        this.project = project;
    }

    public static synchronized MyToolWindowUpdater getInstance(Project project) {
        if (instance == null) {
            instance = new MyToolWindowUpdater(project);
        }
        return instance;
    }

    public void refreshToolWindow() {

        if (!project.isDisposed()) {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("TestingToolWindow");
            if (toolWindow != null) {
                LogcatAnalyzerToolWindowFactory.refreshToolWindow();
            }
        }else {
            return;
        }


    }
}
