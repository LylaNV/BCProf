package com.github.lylanv.secdroid.inspections;

import com.intellij.ant.PrefixedPath;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtImportList;
import org.jetbrains.kotlin.psi.KtImportDirective;

public class ImportChecker {
    private Project project; //Holds the project
    private PsiFile psiFile; //Holds the input PsiFile
    private PsiImportList importList; //Holds the list of the imports in the input Java file
    private KtImportList importListKotlin; //Holds the list of the imports in the input Kotlin file
    private PsiJavaFile javaFile; //Holds the input java file
    private KtFile kotlinFile; //Holds the input java file
    private PsiImportStatement[] importStatements; //Holds the import statements of the import list
    private Boolean importIsAvailable; //Determines if the import is already exist

    private String fileLanguage;

    public ImportChecker(final Project project) {
        this.project = project;
    }

    // First this method should be called otherwise the values will be null
    // This method checks if the "import android.util.Log;" is already imported/exist
    public boolean checkImports(VirtualFile virtualFile,PsiManager psiManager) {
        psiFile = psiManager.findFile(virtualFile);

        if (psiFile instanceof PsiJavaFile){
            javaFile = (PsiJavaFile) psiFile;
            importList = javaFile.getImportList();

            if (importList != null) {
                importStatements = importList.getImportStatements();
                if (importStatements.length != 0) {
                    for (final PsiImportStatement importStatement : importStatements) {
                        String importText = importStatement.getText().trim();
                        if (importText.equals("import android.util.Log;")) {
                            return importIsAvailable = true;
                        }
                    }
                }
            }
            return importIsAvailable = false;
        }else if (psiFile instanceof KtFile) {
            kotlinFile = (KtFile) psiFile;
            importListKotlin = kotlinFile.getImportList();

            if (importListKotlin != null) {
                for (KtImportDirective importDirective : importListKotlin.getImports()) {
                    String importText = importDirective.getText().trim();
                    if (importText.equals("import ")) {
                        System.out.println("[GreenMeter -> ImportChecker -> checkImports$ Kotlin File- import is available.");
                        return importIsAvailable = true;
                    }
                }
            }
            System.out.println("[GreenMeter -> ImportChecker -> checkImports$ Kotlin File- import is not found.");
            return importIsAvailable = false;
        } else {
            System.out.println("[GreenMeter -> ImportChecker -> checkImports$ Fatal Error: The file is not a Kotlin or Java file.");
            return importIsAvailable = false;
        }
    }

    // This method adds the required/missing import statement ("import android.util.Log;") for log
    public void addLogImportStatement(VirtualFile virtualFile) {

        WriteCommandAction.runWriteCommandAction(project,(Runnable) () -> {
            Document inputDocument = psiFile.getViewProvider().getDocument();
            String importStatement = "\nimport android.util.Log;\n";

            //TODO: check this it is not working if there is not free space!
            // Insert the import statement after the first line break
            int firstLineBreak = psiFile.getText().indexOf("\n");
            if (firstLineBreak > 0) {
                // The reason for  +2 : it adds the import after package and a white line
                //inputDocument.insertString(firstLineBreak + 2, importStatement);
                inputDocument.insertString(firstLineBreak + 1, importStatement);
            } else {
                // No line break found, insert at the beginning
                //Creates an enter/white space element
//                    PsiParserFacade localParserFacade = PsiParserFacade.getInstance(project);
//                    PsiElement emptyLine = localParserFacade.createWhiteSpaceFromText("\n");
                inputDocument.insertString(importStatement.length(), importStatement);
                inputDocument.insertString(0, importStatement);
                //importList.addImportStatement(importStatement);
            }
//            CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
//            codeStyleManager.reformat(inputPsiFile);
        });


//        FileType fileType = virtualFile.getFileType();
//
//        if (fileType instanceof KotlinFileType) {
//            WriteCommandAction.runWriteCommandAction(project,(Runnable) () -> {
//                Document inputDocument = psiFile.getViewProvider().getDocument();
//                String importStatement = "\nimport android.util.Log;\n";
//
//                //TODO: check this it is not working if there is not free space!
//                // Insert the import statement after the first line break
//                int firstLineBreak = psiFile.getText().indexOf("\n");
//                if (firstLineBreak > 0) {
//                    inputDocument.insertString(firstLineBreak + 1, importStatement);
//                } else {
//                    inputDocument.insertString(importStatement.length(), importStatement);
//                    inputDocument.insertString(0, importStatement);
//                }
//            });
//        } else if (fileType instanceof JavaFileType) {
//
//            WriteCommandAction.runWriteCommandAction(project,(Runnable) () -> {
//                Document inputDocument = psiFile.getViewProvider().getDocument();
//                String importStatement = "\nimport android.util.Log;\n";
//
//                //TODO: check this it is not working if there is not free space!
//                // Insert the import statement after the first line break
//                int firstLineBreak = psiFile.getText().indexOf("\n");
//                if (firstLineBreak > 0) {
//                    // The reason for  +2 : it adds the import after package and a white line
//                    //inputDocument.insertString(firstLineBreak + 2, importStatement);
//                    inputDocument.insertString(firstLineBreak + 1, importStatement);
//                } else {
//                    // No line break found, insert at the beginning
//                    //Creates an enter/white space element
////                    PsiParserFacade localParserFacade = PsiParserFacade.getInstance(project);
////                    PsiElement emptyLine = localParserFacade.createWhiteSpaceFromText("\n");
//                    inputDocument.insertString(importStatement.length(), importStatement);
//                    inputDocument.insertString(0, importStatement);
//                    //importList.addImportStatement(importStatement);
//                }
////            CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
////            codeStyleManager.reformat(inputPsiFile);
//            });
//
//        } else {
//            System.out.println("[GreenMeter -> ImportChecker -> addLogImportStatement$ Fatal Error: Couldn't add the import log statement because the file is not a Kotlin or Java file.");
//        }


    }
}
