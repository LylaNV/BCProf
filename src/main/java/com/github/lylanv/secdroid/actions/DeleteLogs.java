package com.github.lylanv.secdroid.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
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

        //Gets the classes in each file in the project
        for (VirtualFile virtualFile : containingFiles) {
            /*
             * By this "if", we exclude all java and kotlin files that are not in the main folder of the project such as test files
             * to be more precise androidTest and test
             * Filters the Java files in the project to access the Java files with actual source code of the application
             * */
            if (virtualFile.getUrl().contains("/src/main")){
                PsiManager psiManager = PsiManager.getInstance(project);
                PsiFile psiFile = psiManager.findFile(virtualFile);

                if (psiFile instanceof PsiJavaFile) {
                    psiClasses = convertVirtualFileToPsiClass(project,virtualFile);
                    if (psiClasses == null || psiClasses.length == 0) {
                        System.out.println("[GreenMeter -> DeleteLogs -> actionPerformed$ Could not retrieve the classes in the file " + virtualFile.getName().trim());
                    }else {
                        for (PsiClass psiClass : psiClasses) {
                            //Get all the methods in the class
                            psiMethods = psiClass.getMethods();
                            if (psiMethods != null) {
                                for (PsiMethod psiMethod : psiMethods) {
                                    //Get method body
                                    PsiCodeBlock methodBody = psiMethod.getBody();
                                    if (methodBody != null) {
                                        PsiStatement[] statements = methodBody.getStatements();

                                        for (PsiStatement statement : statements) {
                                            String text = statement.getText();
                                            if (text.contains("Log.d(\"GreenMeter\"")) { //&& (text.contains("METHOD_START") || text.contains("METHOD_END") || text.contains(psiMethod.getName()))
                                                javaStatementsToDelete.add(statement);
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                }else if (psiFile instanceof KtFile) {

                    List<KtClass> ktClasses = convertVirtualFileToKotlinClass(project,virtualFile);

                    if (ktClasses == null || ktClasses.size() == 0) {
                        System.out.println("[GreenMeter -> DeleteLogs -> actionPerformed$ Could not retrieve the classes in the Kotlin file " + virtualFile.getName().trim());
                    }else {
                        //Extracts the class from the classes list
                        for (KtClass kotlinClass : ktClasses) {
                            // functions: Holds all the functions inside the input kotlin file and a specific class in that
                            List<KtNamedFunction> functions = new ArrayList<>();
                            // Get all declarations in the class
                            for (KtDeclaration declaration : kotlinClass.getDeclarations()) {
                                // Filter for functions
                                if (declaration instanceof KtNamedFunction) {
                                    functions.add((KtNamedFunction) declaration);
                                }
                            }

                            if (functions.size() > 0) {
                                for (KtNamedFunction function : functions) {
                                    KtExpression functionBody = function.getBodyExpression();

                                    if (functionBody != null) {
                                        KtBlockExpression functionBodyBlock = function.getBodyBlockExpression();
                                        List<KtExpression> ktExpressions = functionBodyBlock.getStatements();

                                        if(ktExpressions.size() > 0) {
                                            for (KtExpression ktExpression : ktExpressions) {
                                                String textKotlin = ktExpression.getText();
                                                if (textKotlin.contains("Log.d(\"GreenMeter\"")) { //&& (textKotlin.contains("METHOD_START") || textKotlin.contains("METHOD_END") || textKotlin.contains(function.getName()))
                                                    kotlinExpressionsToDelete.add(ktExpression);
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        }


                    }
                } else {
                    System.out.println("[GreenMeter -> DeleteLogs -> actionPerformed$ Fatal Error: unknown file type: " + virtualFile.getName().trim());
                }
            }
        }

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
