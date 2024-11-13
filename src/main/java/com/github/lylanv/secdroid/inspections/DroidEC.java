package com.github.lylanv.secdroid.inspections;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.xml.ConvertContext;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DroidEC extends AnAction {
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
    PsiAnnotation annotation; //Holds annotation
    PsiDirectory projectDirectory; //Holds the project directory
    ImportChecker importChecker; //Holds an instance of ImportChecker class -> this variable is used to check the list of the imports in the project and add any missing one
    Boolean importLogStatementAvailable; //Determines if there is any missing import
    private final String Logging_TAG = "GreenMeter"; //A TAG that we use in adding logs, so we can differentiate our added logs from rest of logs
    private final String MethodStart_TAG = "METHOD_START";
    private final String MethodEnd_TAG = "METHOD_END";

    public static Singleton singleton; //Holds none changeable and needed variables by other classes such as project

    String START_OF_METHOD_ANNOTATION_CLASS = "StartOfMethod"; //Holds annotation text

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {

        System.out.println("[GreenMeter -> actionPerformed$ GreenMeter button is clicked");

        //fillRedAPICallsSet();

        //Gets the project
        project = event.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            System.out.println("[GreenMeter -> actionPerformed$ Fatal error: project is null");
            return;
        } else {
            //TODO: Singleton is not recommended in plugin development, consider to remove it.
            projectName = project.getName();
            singleton = new Singleton();
        }

        //Initiates the importChecker -> this variable is used to check the list of the imports in the project and add any missing one
        importChecker = new ImportChecker(project);
        if (importChecker == null){
            System.out.println("[GreenMeter -> actionPerformed$ Fatal error: importChecker is null");
            return;
        }

        //Gets the PsiParserFacade to be able to create white spaces elements
        parserFacade = PsiParserFacade.getInstance(project);
        if (parserFacade == null){
            System.out.println("[GreenMeter -> actionPerformed$ Fatal error: parserFacade is null");
            System.out.println("[GreenMeter -> actionPerformed$ Fatal error: PSI tree cannot be manipulated!");
            return;
        }

        //Gets the PsiElement Factory
        factory = JavaPsiFacade.getElementFactory(project);

        //Gets the PSI manager
        psiManager = PsiManager.getInstance(project);
        if (psiManager == null) {
            System.out.println("[GreenMeter -> actionPerformed$ Fatal error: psiManager is null");
            return;
        }

        //Gets the editor
        editor = event.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            System.out.println("[GreenMeter -> actionPerformed$ Fatal error: editor is null");
            return;
        }

        //Gets the psiFile
        psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile == null) {
            System.out.println("[GreenMeter -> actionPerformed$ Fatal error: psiFile is null");
            return;
        }

//        psiFile.accept(new PsiRecursiveElementWalkingVisitor() {
//            @Override
//            public void visitElement(PsiElement element) {
//                super.visitElement(element);
//
//                // Check if the element is a semicolon token
//                if (element instanceof LeafPsiElement) {
//                    LeafPsiElement leaf = (LeafPsiElement) element;
//                    if (leaf.getElementType() == JavaTokenType.SEMICOLON) {
//                        System.out.println("Found a semicolon: " + leaf.getText());
//                    }
//                }
//            }
//        });

//        PsiElementVisitor visitor = new PsiElementVisitor() {
//            @Override
//            public void visitElement(@NotNull PsiElement element){
//                if (element.getNode().getElementType() == JavaTokenType.SEMICOLON){
//                    System.out.println("[GreenMeter -> logFindViewById$ HURRRRAAAAA I FOUND A ;");
//                }
//            }
//        };
//        psiFile.accept(visitor);


        //Gets the document
        Document document = (Document) PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            System.out.println("[GreenMeter -> actionPerformed$ Fatal error: document is null");
            return;
        }

//        //Gets the project directory
//        projectDirectory = (PsiDirectory) project.getBaseDir();
//        if (projectDirectory == null) {
//            System.out.println("[GreenMeter -> actionPerformed$ Fatal error: projectDirectory is null");
//            return;
//        }else {
//            System.out.println("[GreenMeter -> actionPerformed$ projectDirectory is " + projectDirectory.getName());
//        }


        //createAnnotationFile(project,projectDirectory,START_OF_METHOD_ANNOTATION_CLASS);

        /*
        * Gets all the Java files in the project even the test files
        * https://intellij-support.jetbrains.com/hc/en-us/community/posts/360009512280-Find-all-PsiClasses-in-Project
        * containingFiles = FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME, JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project));
        * Since FileTypeIndex.NAME is deprecated, it is replaced by following line of code.
        * */
        containingFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project));

        if (containingFiles.toArray().length == 0) {
            System.out.println("[GreenMeter -> actionPerformed$ Fatal error: there is no file in the project!");
            return;
        }

        //Gets the classes in each file in the project
        for (VirtualFile virtualFile : containingFiles) {
            /*
             * By this "if", we exclude all java files that are not in the main folder of the project such as test files
             * to be more precise androidTest and test
             * Filters the Java files in the project to access the Java files with actual source code of the application
             * */
            if (virtualFile.getUrl().contains("/src/main")){
                psiClasses = convertVirtualFileToPsiClass(project,virtualFile);
                if (psiClasses.length == 0) {
                    System.out.println("[GreenMeter -> actionPerformed$ Could not retrieve the java classes in the project or project does not have any java class.");
                }else {
                    //retrieveClasses(psiClasses); //Calls methods that annotate methods - WORKING
                    //Determines if there is any missing import
                    //Be careful, we need to first call the checkImports function then the addLogImportStatement function
                    importLogStatementAvailable = importChecker.checkImports(virtualFile,psiManager);
                    //To detect methods in the code we first need to extract class in the source code
                    logMethodsStart(psiClasses);


                    analyzeAndroidAPIs(virtualFile);

                    /*
                    * IMPORTANT NOTE: Missing imports should be added after log statements; otherwise, we will get error that we try to change the un-commited document
                    * Add the import log statement if it is not exist
                    * */
                    if (!importLogStatementAvailable) {
                        importChecker.addLogImportStatement();
                    }

                }
            }
        }
    }


    // This method converts VirtualFiles to psiClass
    private static PsiClass[] convertVirtualFileToPsiClass(Project project, VirtualFile virtualFile) {
        if (virtualFile == null) {
            System.out.println("[GreenMeter -> actionPerformed -> convertVirtualFileToPsiClass$ Fatal error: VirtualFile is null");
            return null;
        }

        if (!virtualFile.getName().endsWith(".java")) {
            System.out.println("[GreenMeter -> actionPerformed -> convertVirtualFileToPsiClass$ Fatal error: VirtualFile is not Java file type");
            return null;
        }

        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile psiFile = psiManager.findFile(virtualFile);

        if(psiFile == null || !(psiFile instanceof PsiJavaFile)) {
            System.out.println("[GreenMeter -> actionPerformed -> convertVirtualFileToPsiClass$ Fatal error: The psiFile related to virtualFile could not be found.");
            return null;
        }

        PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
        PsiClass[] classes = psiJavaFile.getClasses();
        if (classes.length == 0) {
            System.out.println("[GreenMeter -> actionPerformed -> convertVirtualFileToPsiClass $ Fatal error: There is no class associated to the input virtual file.");
            return null;
        }

        return classes;
    }


    private void logMethodsStart(PsiClass[] psiClasses) {
        //Extracts the class from the classes array
        for (PsiClass psiClass : psiClasses) {
            String className = psiClass.getName();
            System.out.println("[GreenMeter -> actionPerformed -> logMethodsStart$ The class name is " + className);

            //Get all the methods in the class
            psiMethods = psiClass.getMethods();

            for (PsiMethod psiMethod : psiMethods) {
                String methodName = psiMethod.getName();
                System.out.println("[GreenMeter -> actionPerformed -> logMethodsStart$ The method name is " + methodName);

                //Get method body
                PsiCodeBlock methodBody = psiMethod.getBody();

                if (methodBody == null) {
                    System.out.println("[GreenMeter -> actionPerformed -> logMethodsStart$ The " + methodName + " method is empty so it will not consume energy!");
                }else {

                    //Extract Android API calls from the method body
                    retrieveAPICallsInMethod(methodBody);

                    //Generate the method start log statement
                    String startLogStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + methodName + "," + className + "," + MethodStart_TAG + ")\");";
                    PsiStatement startLogStatementElement = factory.createStatementFromText(startLogStatement,psiMethod);

                    //Generate the method end log statement
                    String endLogStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + methodName + "," + className + "," + MethodEnd_TAG + ")\");";
                    PsiStatement endLogStatementElement = factory.createStatementFromText(endLogStatement,psiMethod);

                    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);

                    //Add the method start log statement
                    WriteCommandAction.runWriteCommandAction(project, (Runnable) () -> {methodBody.addBefore(startLogStatementElement, methodBody.getFirstBodyElement());});

                    //Add the method end log statement
                    WriteCommandAction.runWriteCommandAction(project, (Runnable) () -> {methodBody.add(endLogStatementElement);});
                }
            }
        }
    }

    public static void retrieveAPICallsInMethod(PsiCodeBlock methodBody) {
        List<PsiMethodCallExpression> methodCalls = new ArrayList<>();

        methodCalls.addAll(PsiTreeUtil.collectElementsOfType(methodBody, PsiMethodCallExpression.class));

        for (PsiMethodCallExpression psiMethodCallExpression : methodCalls) {
            PsiReferenceExpression methodExpression = psiMethodCallExpression.getMethodExpression();
            String methodName = methodExpression.getReferenceName();  // Get the method name
            System.out.println("-----------Method call found: " + methodName);
        }

    }

//    // This method annotates methods - WORKING
//    private void retrieveClasses(PsiClass[] psiClasses) {
//        for (PsiClass psiClass : psiClasses) {
//            annotateMethods(psiClass);
//        }
//    }
//
//    // This method annotates methods - WORKING
//    private void annotateMethods(PsiClass psiClass) {
//
//        psiMethods = psiClass.getMethods();
//        for (PsiMethod psiMethod : psiMethods) {
//            System.out.println("[GreenMeter -> actionPerformed -> annotateClasses -> annotateMethods$ psiMethod is " + psiMethod);
//            //factory = JavaPsiFacade.getElementFactory(psiMethod.getProject());
//            annotation = factory.createAnnotationFromText("@StartOfMethod", psiMethod);
//            //annotation = factory.createAnnotationFromText("@com.github.lylanv.greenedge.inspections.StartOfMethod", psiMethod);
//
//            new WriteCommandAction.Simple(project, psiMethod.getContainingFile()) {
//                @Override
//                protected void run() throws Throwable {
//                    PsiModifierList modifierList = psiMethod.getModifierList();
//                    PsiElement firstChild = modifierList.getFirstChild();
//
//                    if (modifierList != null) {
//
//                        // Gets the methods annotations list
//                        PsiAnnotation[] annotations = modifierList.getAnnotations();
//
//                        // The following "if" will be executed if the method has annotations
//                        if (annotations.length > 0) {
//                            // Find the first child of modifierList that is not an annotation
//                            PsiElement insertionPoint = null;
//                            PsiElement[] children = modifierList.getChildren();
//                            for (PsiElement child : children) {
//                                //TODO: check if this part of the code can also detects Android specific annotations as well
//                                if (!(child instanceof PsiAnnotation)) {
//                                    insertionPoint = child;
//                                    break;
//                                }
//                            }
//                            if (insertionPoint != null) {
//                                // Add the annotation before the insertion point
//                                PsiElement finalInsertionPoint = insertionPoint;
//                                WriteCommandAction.runWriteCommandAction(project, (Runnable) () -> {modifierList.addBefore(annotation, finalInsertionPoint);});
//                            } else {
//                                // If all children are annotations, add the annotation at the end of the modifier list
//                                WriteCommandAction.runWriteCommandAction(project, (Runnable) () -> {modifierList.add(annotation);});
//                            }
//                        } else {
//                            if (firstChild instanceof PsiKeyword) {
//                                // Add the annotation before the "public" modifier
//                                WriteCommandAction.runWriteCommandAction(project, (Runnable) () -> {modifierList.addBefore(annotation, firstChild);});
//                            } else {
//                                // If there is no existing modifier, or it's not a PsiKeyword, simply add the annotation to the modifier list
//                                WriteCommandAction.runWriteCommandAction(project, (Runnable) () -> {modifierList.add(annotation);});
//                            }
//                        }
//                    } else {
//                        WriteCommandAction.runWriteCommandAction(project, (Runnable) () -> {modifierList.add(annotation);});
//                    }
//                }
//            }.execute();
//        }
//
//    }

    // This method travers the input virtual file and finds method call
    // and filters specific API calls and adds the proper log statements
    private void analyzeAndroidAPIs(VirtualFile inputVirtualFile) {
        PsiFile inputPsiFile = psiManager.findFile(inputVirtualFile);
        if (inputPsiFile != null) {
            System.out.println("[GreenMeter -> analyzeAndroidAPIs$ psiFile name is " + inputPsiFile.getName());
            inputPsiFile.accept(new JavaRecursiveElementWalkingVisitor() {
                @Override
                public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                    super.visitMethodCallExpression(expression);

//                    PsiExpressionList argumentList = expression.getArgumentList();
//                    if (argumentList != null) {
//                        PsiExpression[] arguments = argumentList.getExpressions();
//                        if (arguments.length > 0) {
//                            PsiExpression firstArgument = arguments[0];
//                            PsiElement parentElement = expression.getParent();
//                            System.out.println("[GreenMeter -> analyzeAndroidAPIs$ firstArgument is " + firstArgument);
//                            String test = parentElement.getText().replace(firstArgument.getText(),"");
//                            System.out.println("[GreenMeter -> analyzeAndroidAPIs$ parentElement is " + test);
//                            System.out.println("[GreenMeter -> analyzeAndroidAPIs$ expression is " + expression.getText());
//
//                        }
//                    }

                    String fileName = inputPsiFile.getName();
                    String methodCallName = expression.getMethodExpression().getReferenceName();
                    System.out.println("[GreenMeter -> analyzeAndroidAPIs$ methodCallName is " + methodCallName);

                    // This returns method call and the part before that together, for example Log.d or mReplyTextView.setText
                    String methodCallNameExtended = expression.getMethodExpression().getReference().getCanonicalText();
                    System.out.println("[GreenMeter -> analyzeAndroidAPIs$ methodCallName parent name is " + expression.getMethodExpression().getReference().getCanonicalText());

//                    // This segment of code logs the method calls - WORKING
//                    if (!methodCallName.equals("d") && singleton.redAPICalls.keySet().contains(methodCallName)) {
//                        addLogStatement(expression,methodCallName, fileName);
//                    } else {
//                        if (singleton.redAPICalls.keySet().contains(methodCallName)) {
//                            PsiExpressionList argumentList = expression.getArgumentList();
//                            if (argumentList != null) {
//                                PsiExpression[] arguments = argumentList.getExpressions();
//                                PsiExpression firstArgument = arguments[0];
//                                if (!firstArgument.getText().contains(Logging_TAG)) {
//                                    addLogStatement(expression,methodCallName, fileName);
//                                }
//                            }else {
//                                addLogStatement(expression,methodCallName, fileName);
//                            }
//                        }
//                    }

                    // This segment (if segment) of code logs the method calls - WORKING
                    // Case 1: unique APIs
                    if(singleton.redAPICalls.keySet().contains(methodCallName)){
                        if (!methodCallName.equals("d")){
                            addLogStatement(expression,methodCallName, fileName);
                        }else{
                            if (singleton.redAPICalls.keySet().contains(methodCallName)) {
                                PsiExpressionList argumentList = expression.getArgumentList();
                                if (argumentList != null) {
                                    PsiExpression[] arguments = argumentList.getExpressions();
                                    PsiExpression firstArgument = arguments[0];
                                    if (!firstArgument.getText().contains(Logging_TAG)) {
                                        addLogStatement(expression,methodCallName, fileName);
                                    }
                                }else {
                                    addLogStatement(expression,methodCallName, fileName);
                                }
                            }
                        }

                    }else{

                        if (singleton.hwAPICalls.keySet().contains(methodCallNameExtended)) {
                            //Case 2: hardware APIs
                        }else if (singleton.jointRedAPICalls.keySet().contains(methodCallNameExtended)) {
                            //Case 3: repetitive APIs
                        }else{
                            //Case 4: CPU - Memory intensive
                        }

                    }

                    /*
                     *
                     *
                     *
                     *
                     * STOP
                     * STOP
                     * STOP
                     *
                     *
                     *
                     *
                     * */
//
//                    if (singleton.jointRedAPICalls.keySet().contains(methodCallName)) {
//                        String jointRedAPICallType = expression.getMethodExpression().getReference().getCanonicalText();
//
//                        if (singleton.jointRedAPIsParents.keySet().contains(jointRedAPICallType + "." + methodCallName)) {
//                            addTimeStamp(expression,methodCallName,fileName);
//                        }
//                    }





//                    switch (methodCallName) {
//                        case "performClick":
//                            System.out.println("[GreenMeter -> analyzeAndroidAPIs$ case is: " + methodCallName);
//                            addLogStatement(expression,methodCallName, fileName);
//                            break;
//                        case "getIntExtra":
//                            System.out.println("[GreenMeter -> analyzeAndroidAPIs$ case is: " + methodCallName);
//                            addLogStatement(expression,methodCallName, fileName);
//                            break;
//                        case "i":
//                            System.out.println("[GreenMeter -> analyzeAndroidAPIs$ case is: " + methodCallName);
//                            addLogStatement(expression,methodCallName, fileName);
//                            break;
//                        case "finish":
//                            System.out.println("[GreenMeter -> analyzeAndroidAPIs$ case is: " + methodCallName);
//                            //TODO: add the log statement before finish().
//                            addLogStatement(expression,methodCallName, fileName);
//                            break;
//                        case "cancelAll":
//                            System.out.println("[GreenMeter -> analyzeAndroidAPIs$ case is: " + methodCallName);
//                            addLogStatement(expression,methodCallName, fileName);
//                            break;
//                        case "startActivityForResult":
//                            System.out.println("[GreenMeter -> analyzeAndroidAPIs$ case is: " + methodCallName);
//                            addLogStatement(expression,methodCallName, fileName);
//                            break;
//                        case "findViewById":
//                            System.out.println("[GreenMeter -> analyzeAndroidAPIs$ case is: " + methodCallName);
//                            addLogStatement(expression,methodCallName, fileName);
//                            break;
//                        case "getPhoneType":
//                            System.out.println("[GreenMeter -> analyzeAndroidAPIs$ case is: " + methodCallName);
//                            addLogStatement(expression,methodCallName, fileName);
//                            break;
//                        case "clear":
//                            System.out.println("[GreenMeter -> analyzeAndroidAPIs$ case is: " + methodCallName);
//                            addLogStatement(expression,methodCallName, fileName);
//                            break;
//                        case "getPixel":
//                            System.out.println("[GreenMeter -> analyzeAndroidAPIs$ case is: " + methodCallName);
//                            addLogStatement(expression,methodCallName, fileName);
//                            break;
//                        default:
//                            System.out.println("[GreenMeter -> analyzeAndroidAPIs$ case is default");
//
////                                case :
////                                case :
////                                case :
////                                case :
////                                case :
////                                case :
////                                case :
////                                case :
////                                case :
////                                case :
////                                case :
//                    }


//                    CommandProcessor.getInstance().executeCommand(project, (Runnable) () -> {
//                        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
//                        codeStyleManager.reformat(psiFile);
//                    },"Reformat Code",null);


                }
            });
        }else {
            System.out.println("[GreenMeter -> annotateMethods$ There is not equivalent PSI file for input virtual file.");
        }
    }

    //This method creates and adds the log statements to the source code of the application
    private void addLogStatement(PsiMethodCallExpression expression, String methodCallName, String javaFile) {

        System.out.println("[GreenMeter -> addLogStatement$ addLogStatement method is called");

        //Finds the complete method call expression (I mean the one with semicolon)
        PsiElement parent = expression.getParent();
        while (parent != null && !(parent instanceof PsiStatement)) {
            parent = parent.getParent();
        }

        if (parent != null) {
            int lineNumber; // Holds the exact line number of the API call
            if (importLogStatementAvailable){
                lineNumber = getLineNumber(parent) + 1;
            }else {
                lineNumber = getLineNumber(parent) + 3;
            }

            //String logStatement = "Log.d(\"" + Logging_TAG + "\", \"" + methodCallName + ", File: " + javaFile + ", Line number is " + lineNumber + "\");";
            String logStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + methodCallName + "," + javaFile + "," + lineNumber + ")\");";
            PsiStatement logStatementElement = factory.createStatementFromText(logStatement,expression.getContext());

            //The semicolon should be in one of the leaf nodes
            PsiElement semicolon = PsiTreeUtil.nextLeaf(parent);

            //Finds the semicolon which is in the end of the method call expression
            while (semicolon != null && !(semicolon instanceof PsiJavaToken && ((PsiJavaToken) semicolon).getTokenType() == JavaTokenType.SEMICOLON)) {
                //semicolon = PsiTreeUtil.nextLeaf(semicolon);
                semicolon = PsiTreeUtil.prevLeaf(semicolon);
            }

            //Creates an enter/white space element
            PsiElement emptyLine = parserFacade.createWhiteSpaceFromText("\n");

            //writes the statement and the white space to the right place in the Psi tree
            PsiElement finalInsertionPoint = semicolon.getParent();
            PsiElement finalSemicolon = semicolon;
            CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
            WriteCommandAction.runWriteCommandAction(project, (Runnable) () -> {finalInsertionPoint.addAfter(logStatementElement,finalSemicolon);});
            //TODO: change adding white sapace manually. We should not do that beased on: https://plugins.jetbrains.com/docs/intellij/modifying-psi.html#whitespaces-and-imports
            WriteCommandAction.runWriteCommandAction(project, (Runnable) () -> {finalInsertionPoint.addAfter(emptyLine, finalSemicolon);});
//            WriteCommandAction.runWriteCommandAction(project,(Runnable) () -> {codeStyleManager.reformatNewlyAddedElement((ASTNode) logStatementElement.getParent().getNode(),logStatementElement.getNode());});
//            WriteCommandAction.runWriteCommandAction(project,(Runnable) () -> {codeStyleManager.reformatNewlyAddedElement(psiFile.getNode(), finalInsertionPoint.getNode());});
        }else {
            System.out.println("[GreenMeter -> logFindViewById$ Fatal error: Method call expression is null: There is not any method call!");
        }
    }

    private void addTimeStamp(PsiMethodCallExpression expression, String methodCallName, String javaFile) {

    }

    //This method returns the line number of the input element in the editor
    private int getLineNumber(PsiElement element) {

        int startOffset = element.getTextRange().getStartOffset();

        PsiFile elementFile = element.getContainingFile();
        if (elementFile == null){
            return -1;
        }else {
            Document elementDocument = PsiDocumentManager.getInstance(elementFile.getProject()).getDocument(elementFile);
            if (elementDocument == null){
                return -1;
            }else {
                return elementDocument.getLineNumber(startOffset);
            }
        }
    }
}
