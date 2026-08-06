package com.github.lylanv.secdroid.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.kotlin.psi.*;

import java.util.*;


public class DeleteLogs extends AnAction {

    private final String Logging_TAG = "BPDroid";
    private final String LANGUAGE_JAVA = "java";
    private final String LANGUAGE_KOTLIN = "kotlin";
    Project project; //Holds the project
    public static String projectName;
    PsiParserFacade parserFacade; //Holds the PsiParserFacade
    Editor editor; //Holds the editor
    PsiFile psiFile; //Holds PsiFile
    PsiManager psiManager; //Holds PsiManager
    Collection<VirtualFile> containingFiles; //Holds virtual files in the project
    PsiClass[] psiClasses; //Holds the classes in the project
    PsiMethod[] psiMethods; //Holds the list of the methods in the project
    PsiElementFactory factory; //Holds PsiElementFactory
    KtPsiFactory factoryKotlin;

    List<PsiStatement> javaStatementsToDelete;
    List<KtExpression> kotlinExpressionsToDelete;

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        System.out.println("[GreenMeter -> actionPerformed$ Delete BCProf Logs button is clicked");

        //Gets the project
        project = anActionEvent.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            System.out.println("[GreenMeter -> DeleteLogs -> actionPerformed$ Fatal error: project is null");
            return;
        } else {
            projectName = project.getName();
        }

        //Gets the PsiParserFacade to be able to create white spaces elements
        parserFacade = PsiParserFacade.getInstance(project);
        if (parserFacade == null){
            System.out.println("[GreenMeter -> DeleteLogs -> actionPerformed$ Fatal error: parserFacade is null");
            System.out.println("[GreenMeter -> DeleteLogs -> actionPerformed$ Fatal error: PSI tree cannot be manipulated!");
            return;
        }

        //Gets the PsiElement Factory
        factory = JavaPsiFacade.getElementFactory(project);
        //Gets KtPsiFactory for kotlin
        factoryKotlin = new KtPsiFactory(project);

        //Gets the PSI manager
        psiManager = PsiManager.getInstance(project);
        if (psiManager == null) {
            System.out.println("[GreenMeter -> DeleteLogs -> actionPerformed$ Fatal error: psiManager is null");
            return;
        }

        //Gets the editor
        editor = anActionEvent.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            System.out.println("[GreenMeter -> DeleteLogs -> actionPerformed$ Fatal error: editor is null");
            return;
        }

        //Gets the psiFile
        psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile == null) {
            System.out.println("[GreenMeter -> DeleteLogs -> actionPerformed$ Fatal error: psiFile is null");
            return;
        }

        //Gets the document
        Document document = (Document) PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            System.out.println("[GreenMeter -> DeleteLogs -> actionPerformed$ Fatal error: document is null");
            return;
        }

        //Gets all the Java and Kotlin files in the project /src/main folder in the form of virtual file
        containingFiles = getAllJavaAndKotlinFiles(project);

        if (containingFiles.toArray().length == 0) {
            System.out.println("[GreenMeter -> DeleteLogs -> actionPerformed$ Fatal error: there is no file in the project!");
            return;
        }

        javaStatementsToDelete = new ArrayList<>();
        kotlinExpressionsToDelete = new ArrayList<>();

        for (VirtualFile virtualFile : containingFiles) {
            if (virtualFile.getUrl().contains("/src/main")) {
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile == null) continue;

                if (psiFile instanceof PsiJavaFile) {
                    removeGreenMeterLogsFromJava((PsiJavaFile) psiFile);
                } else if (psiFile instanceof KtFile) {
                    removeGreenMeterLogsFromKotlin((KtFile) psiFile);
                }
            }
        }

        //------------------------------------------------------------------

        if (!javaStatementsToDelete.isEmpty()) {

            WriteCommandAction.runWriteCommandAction(project,"Delete GreenMeter Java Logs", null, () -> {
                for (PsiStatement psiStatement : javaStatementsToDelete) {
                    if (psiStatement.isValid()) {
                        psiStatement.delete();
                    }
                }
            });
        }

        if(!kotlinExpressionsToDelete.isEmpty()) {
            WriteCommandAction.runWriteCommandAction(project,"Delete GreenMeter Kotlin Logs", null, () -> {
                for (KtExpression ktExpression : kotlinExpressionsToDelete) {
                    if (ktExpression.isValid()) {
                        ktExpression.delete();
                    }
                }
            });
        }
    }


    //------------------------------------------------------------------
    private void removeGreenMeterLogsFromJava(PsiJavaFile javaFile) {
        List<PsiStatement> statementsToDelete = new ArrayList<>();

        // 1. Gather all target statements
        javaFile.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);

                String methodName = expression.getMethodExpression().getReferenceName();
                if ("d".equals(methodName)) {
                    PsiExpressionList argumentList = expression.getArgumentList();
                    if (argumentList != null) {
                        PsiExpression[] arguments = argumentList.getExpressions();
                        if (arguments.length > 0) {
                            String firstArgText = arguments[0].getText();
                            if (firstArgText.contains(Logging_TAG)) {
                                // Find the enclosing PsiStatement parent layer so we extract the entire statement including its semicolon
                                PsiElement statementParent = PsiTreeUtil.getParentOfType(expression, PsiStatement.class);
                                if (statementParent != null) {
                                    statementsToDelete.add((PsiStatement) statementParent);
                                }
                            }
                        }
                    }
                }
            }
        });

        // 2. Delete the statements safely
        if (!statementsToDelete.isEmpty()) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                for (PsiStatement statement : statementsToDelete) {
                    if (statement.isValid()) {
                        // Clean up potential trailing whitespace formatting around the statement
                        PsiElement nextSibling = statement.getNextSibling();
                        if (nextSibling instanceof PsiWhiteSpace) {
                            nextSibling.delete();
                        }
                        statement.delete();
                    }
                }
            });
        }
    }
    //------------------------------------------------------------------

    //------------------------------------------------------------------

    private void removeGreenMeterLogsFromKotlin(KtFile ktFile) {
        List<PsiElement> elementsToDelete = new ArrayList<>();

        // 1. First, gather only the exact GreenMeter log expressions
        ktFile.accept(new KtTreeVisitorVoid() {
            @Override
            public void visitCallExpression(@NotNull KtCallExpression expression) {
                super.visitCallExpression(expression);

                KtExpression calleeExpression = expression.getCalleeExpression();
                String functionName = (calleeExpression != null) ? calleeExpression.getText() : null;

                // Target Log.d statements
                if ("d".equals(functionName)) {
                    KtValueArgumentList argumentList = expression.getValueArgumentList();
                    if (argumentList != null) {
                        List<KtValueArgument> arguments = argumentList.getArguments();
                        if (!arguments.isEmpty()) {
                            String firstArgText = arguments.get(0).getText();
                            // Verify it is specifically a GreenMeter log
                            if (firstArgText.contains(Logging_TAG)) {
                                // If it's part of a qualified expression (e.g., Log.d), we must delete the parent expression container
                                if (expression.getParent() instanceof KtDotQualifiedExpression) {
                                    elementsToDelete.add(expression.getParent());
                                } else {
                                    elementsToDelete.add(expression);
                                }
                            }
                        }
                    }
                }
            }
        });

        // 2. Perform safe, isolated deletion of the gathered elements
        if (!elementsToDelete.isEmpty()) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                for (PsiElement element : elementsToDelete) {
                    if (element.isValid()) {
                        // Check if there is a trailing whitespace/newline right after the log to clean up formatting
                        PsiElement nextSibling = element.getNextSibling();
                        if (nextSibling instanceof PsiWhiteSpace) {
                            nextSibling.delete();
                        }
                        element.delete();
                    }
                }
            });
        }
    }

    //------------------------------------------------------------------


    private Collection<VirtualFile> getAllJavaAndKotlinFiles(Project project) {
        Collection<VirtualFile> allFiles = new ArrayList<>();
        FileBasedIndex.getInstance().iterateIndexableFiles(file -> {
            allFiles.add(file);
            return true;
        }, project, null);

        Collection<VirtualFile> filteredFiles = new ArrayList<>();
        for (VirtualFile file : allFiles) {
            //Only gets the files inside the project which are in main folder
            if (file.getUrl().contains("/src/main")){
                PsiFile psiFileLocal = PsiManager.getInstance(project).findFile(file);
                if (psiFileLocal != null) {
                    String languageId = psiFileLocal.getLanguage().getID();
                    if ("JAVA".equals(languageId) || "kotlin".equals(languageId)) {
                        filteredFiles.add(file);
                    }
                }
            }
        }

        return filteredFiles;
    }


    // This method converts VirtualFiles to psiClass (Java classes)
    private static PsiClass[] convertVirtualFileToPsiClass(Project project, VirtualFile virtualFile) {
        if (virtualFile == null) {
            System.out.println("[GreenMeter -> DeleteLogs -> convertVirtualFileToPsiClass$ Fatal error: VirtualFile is null");
            return null;
        }

        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile psiFile = psiManager.findFile(virtualFile);

        if(psiFile == null) {
            System.out.println("[GreenMeter -> DeleteLogs -> convertVirtualFileToPsiClass$ Fatal error: The psiFile related to virtualFile could not be found.");
            return null;
        }

        PsiClass[] classes;
        if (psiFile instanceof PsiJavaFile) {
            PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
            classes = psiJavaFile.getClasses();
            if (classes.length == 0) {
                System.out.println("[GreenMeter -> DeleteLogs -> convertVirtualFileToPsiClass $ Fatal error: There is no class associated to the input virtual file.");
                return null;
            }
            return classes;
        }else {
            System.out.println("[GreenMeter -> DeleteLogs -> convertVirtualFileToPsiClass $ Fatal error: There is no class associated to the input virtual file.");
            System.out.println("[GreenMeter -> DeleteLogs -> convertVirtualFileToPsiClass $ Fatal error: The file is neither Java nor Kotlin.");
            return null;
        }
    }

    // This method converts VirtualFiles to psiClass (Kotlin classes)
    private static List<KtClass> convertVirtualFileToKotlinClass(Project project, VirtualFile virtualFile) {
        if (virtualFile == null) {
            System.out.println("[GreenMeter -> DeleteLogs -> convertVirtualFileToKotlinClass$ Fatal error: VirtualFile is null");
            return null;
        }

        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile psiFile = psiManager.findFile(virtualFile);
        if(psiFile == null) {
            System.out.println("[GreenMeter -> DeleteLogs -> convertVirtualFileToKotlinClass$ Fatal error: The psiFile related to virtualFile could not be found.");
            return null;
        }

        KtFile psiKtFile = (KtFile) psiFile;
        if (psiKtFile == null) {
            System.out.println("[GreenMeter -> DeleteLogs -> convertVirtualFileToKotlinClass$ Fatal error: The psiKtFile related to virtualFile could not be found.");
            return null;
        }

        List<KtClass> kotlinClasses = new ArrayList<>();
        // Iterate over all top-level elements in the Kotlin file
        for (KtDeclaration classOrObject : psiKtFile.getDeclarations()) {
            if (classOrObject instanceof KtClass) {
                kotlinClasses.add((KtClass) classOrObject);
            }
        }

//        classes = (KtClass[]) psiKtFile.getClasses();
        if (kotlinClasses.size() == 0 || kotlinClasses == null) {
            System.out.println("[GreenMeter -> DeleteLogs -> convertVirtualFileToKotlinClass$ Fatal error: There is no class associated to the input virtual file.");
            return null;
        }
        return kotlinClasses;
    }
}
