package com.github.lylanv.secdroid.inspections;

import com.android.tools.r8.L;
import com.intellij.ant.PrefixedPath;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtImportList;
import org.jetbrains.kotlin.psi.KtImportDirective;

public class ImportChecker {
    private final String LANGUAGE_JAVA = "java";
    private final String LANGUAGE_KOTLIN = "kotlin";
    private Project project; //Holds the project
    private PsiImportList importList; //Holds the list of the imports in the input Java file
    private KtImportList importListKotlin; //Holds the list of the imports in the input Kotlin file
    private PsiImportStatement[] importStatements; //Holds the import statements of the import list
    private Boolean importIsAvailable; //Determines if the import is already exist

    public ImportChecker(final Project project) {
        this.project = project;
    }

    // First this method should be called otherwise the values will be null
    // This method checks if the "import android.util.Log;" is already imported/exist
    public boolean checkImports(@NotNull PsiFile psiFile, @NotNull String fileLanguage) {
        //psiFile = psiManager.findFile(virtualFile);

        if (fileLanguage.equals(LANGUAGE_JAVA)) {
            PsiJavaFile javaFile = (PsiJavaFile) psiFile; //Holds the input java file
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
        }else if (fileLanguage.equals(LANGUAGE_KOTLIN)) {
            KtFile kotlinFile = (KtFile) psiFile; //Holds the input java file
            importListKotlin = kotlinFile.getImportList();

            if (importListKotlin != null) {
                for (KtImportDirective importDirective : importListKotlin.getImports()) {
                    String importText = importDirective.getText().trim();
                    if (importText.equals("import android.util.Log")) {
                        System.out.println("[GreenMeter -> ImportChecker -> checkImports$ Kotlin File- import is available.");
                        return importIsAvailable = true;
                    }
                }
            }
            System.out.println("[GreenMeter -> ImportChecker -> checkImports$ Kotlin File : " + kotlinFile.getName() + "- import is not found.");
            return importIsAvailable = false;
        } else {
            System.out.println("[GreenMeter -> ImportChecker -> checkImports$ Fatal Error: The file is not a Kotlin or Java file.");
            return importIsAvailable = false;
        }
    }

    // This method adds the required/missing import statement ("import android.util.Log;") for log
    public void addLogImportStatement(@NotNull PsiFile psiFile, @NotNull String fileLanguage) {

        WriteCommandAction.runWriteCommandAction(project,(Runnable) () -> {
            Document inputDocument = psiFile.getViewProvider().getDocument();
            String importStatement = null;

            if (fileLanguage.equals(LANGUAGE_KOTLIN)) {
                importStatement = "\nimport android.util.Log\n";
            }else if (fileLanguage.equals(LANGUAGE_JAVA)) {
                importStatement = "\nimport android.util.Log;\n";
            }

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
    }
}
