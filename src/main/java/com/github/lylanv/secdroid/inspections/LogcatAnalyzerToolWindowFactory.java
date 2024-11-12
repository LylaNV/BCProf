package com.github.lylanv.secdroid.inspections;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.util.messages.MessageBusConnection;
import org.apache.maven.model.Build;
import org.jetbrains.annotations.NotNull;

public class LogcatAnalyzerToolWindowFactory implements ToolWindowFactory, DumbAware {
    private Thread logcatAnalyzerThread;
    private LogCatReader logcatReader;
    private final String Logging_TAG = "GreenMeter";
    //Previous TAG was MethodCallProfiler
    private String packageName;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        LogcatAnalyzerToolWindow logcatAnalyzerToolWindow = new LogcatAnalyzerToolWindow(project);
        EventBusManager.register(logcatAnalyzerToolWindow);
        ContentFactory contentFactory = ContentFactory.getInstance();

        // Text tab
        Content textContent = contentFactory.createContent(logcatAnalyzerToolWindow.getTextPanel(), "APIs' Line", false);
        toolWindow.getContentManager().addContent(textContent);

        // Bar graph tab
        Content barGraphContent = contentFactory.createContent(logcatAnalyzerToolWindow.getGraphPanel(), "Bar Graph", false);
        toolWindow.getContentManager().addContent(barGraphContent);

        // Line graph tab
        Content lineGraphContent = contentFactory.createContent(logcatAnalyzerToolWindow.getLineChartPanel(), "Line Graph", false);
        toolWindow.getContentManager().addContent(lineGraphContent);

        // Create new thread to analyze the logcat file
        logcatReader = new LogCatReader(logcatAnalyzerToolWindow,Logging_TAG);
        EventBusManager.register(logcatReader);
        logcatAnalyzerThread = new Thread(logcatReader);
        logcatAnalyzerThread.start();

//        logcatAnalyzerToolWindow.clearGraph();
//        logcatAnalyzerToolWindow.clearTextArea();

        MessageBusConnection connection = project.getMessageBus().connect(project);
        connection.subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void toolWindowShown(ToolWindow window) {
                if (window.getId().equals(toolWindow.getId())) {
                    if (logcatAnalyzerThread == null || !logcatAnalyzerThread.isAlive()) {
                        logcatReader = new LogCatReader(logcatAnalyzerToolWindow,Logging_TAG);
                        EventBusManager.register(logcatReader);
                        logcatAnalyzerThread = new Thread(logcatReader);
                        logcatAnalyzerThread.start();
                    }
                }
            }

//            public void toolWindowHidden(ToolWindow window) {
//                if (window.getId().equals(toolWindow.getId())) {
//                    if (logcatAnalyzerThread != null && logcatAnalyzerThread.isAlive()) {
//                        //logcatReader.stop();
//                        try {
//                            logcatAnalyzerThread.join();
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        logcatAnalyzerThread = null;
//                    }
//                }
//            }
        });
//
//        toolWindow.addContentManagerListener(new ContentManagerListener() {
//            Component selected = toolWindow.getComponent();
//            if (selected != null){
//
//            }
//        });
//
//        toolWindow.addContentManagerListener(new Conte);

    }
}
