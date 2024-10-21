package com.github.lylanv.secdroid.inspections;

import com.intellij.ant.PrefixedPath;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;

public class ImportChecker {
    private Project project; //Holds the project
    private PsiFile psiFile; //Holds the input PsiFile
    private PsiImportList importList; //Holds the list of the imports in the input file
    private PsiJavaFile javaFile; //Holds the input java file
    private PsiImportStatement[] importStatements; //Holds the import statements of the import list
    private Boolean importIsAvailable; //Determines if the import is already exist

    public ImportChecker(final Project project) {
        this.project = project;
    }

    // First this method should be called otherwise the values will be null
    // This method checks if the "import android.util.Log;" is already imported/exist
    public boolean checkImports(VirtualFile virtualFile,PsiManager psiManager) {
        psiFile = psiManager.findFile(virtualFile);
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
    }

    // This method adds the required/missing import statement ("import android.util.Log;") for log
    public void addLogImportStatement() {
        if(!importIsAvailable) {
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
        }
    }
}
