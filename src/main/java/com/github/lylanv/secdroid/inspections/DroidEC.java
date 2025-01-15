package com.github.lylanv.secdroid.inspections;

import com.android.tools.r8.L;
import com.github.lylanv.secdroid.toolWindows.LogcatAnalyzerToolWindowFactory;
import com.github.lylanv.secdroid.utils.ThreeStringKey;
import com.github.lylanv.secdroid.utils.TwoStringKey;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.uast.kotlin.KotlinUBlockExpression;

import java.util.*;

public class DroidEC extends AnAction {
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
    PsiAnnotation annotation; //Holds annotation
    PsiDirectory projectDirectory; //Holds the project directory
    ImportChecker importChecker; //Holds an instance of ImportChecker class -> this variable is used to check the list of the imports in the project and add any missing one
    Boolean importLogStatementAvailable; //Determines if there is any missing import
    private final String Logging_TAG = "GreenMeter"; //A TAG that we use in adding logs, so we can differentiate our added logs from rest of logs
    private final String MethodStart_TAG = "METHOD_START";
    private final String MethodEnd_TAG = "METHOD_END";

    private Map<ThreeStringKey, Integer> methodsAPICallsCountLocalMap = new HashMap<>();
    private Map<TwoStringKey, Double> methodsAPICallsTotalEnergyCostLocalMap = new HashMap<>();

    public static Singleton singleton; //Holds none changeable and needed variables by other classes such as project

    String START_OF_METHOD_ANNOTATION_CLASS = "StartOfMethod"; //Holds annotation text

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {

        System.out.println("[GreenMeter -> actionPerformed$ SECDroid button is clicked");

        //Gets the project
        project = event.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            System.out.println("[GreenMeter -> actionPerformed$ Fatal error: project is null");
            return;
        } else {
            //TODO: Singleton is not recommended in plugin development, consider to remove it.
            projectName = project.getName();
            singleton = new Singleton();

            showPowerXmlFileChooser();
            getBatteryHealthAndCapacity();
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
        //Gets KtPsiFactory for kotlin
        factoryKotlin = new KtPsiFactory(project);

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
        //containingFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project));

        //Gets all the Java and Kotlin files in the project /src/main folder in the form of virtual file
        containingFiles = getAllJavaAndKotlinFiles(project);

        if (containingFiles.toArray().length == 0) {
            System.out.println("[GreenMeter -> actionPerformed$ Fatal error: there is no file in the project!");
            return;
        }

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
                        System.out.println("[GreenMeter -> actionPerformed$ Could not retrieve the classes in the file " + virtualFile.getName().trim());
                    }else {
                        //retrieveClasses(psiClasses); //Calls methods that annotate methods - WORKING
                        //Determines if there is any missing import
                        //Be careful, we need to first call the checkImports function then the addLogImportStatement function
                        importLogStatementAvailable = importChecker.checkImports(psiFile,LANGUAGE_JAVA);

                        //To detect methods in the code we first need to extract class in the source code
                        logMethodsStart(psiClasses, virtualFile);

                        analyzeAndroidAPIs(virtualFile);

                        /*
                         * IMPORTANT NOTE: Missing imports should be added after log statements; otherwise, we will get error that we try to change the un-commited document
                         * Add the import log statement if it is not exist
                         * */
                        if (!importLogStatementAvailable) {
                            importChecker.addLogImportStatement(psiFile,LANGUAGE_JAVA);
                        }
                    }
                }else if (psiFile instanceof KtFile) {

                    List<KtClass> ktClasses = convertVirtualFileToKotlinClass(project,virtualFile);

                    if (ktClasses == null || ktClasses.size() == 0) {
                        System.out.println("[GreenMeter -> actionPerformed$ Could not retrieve the classes in the Kotlin file " + virtualFile.getName().trim());
                    }else {
                        importLogStatementAvailable = importChecker.checkImports(psiFile,LANGUAGE_KOTLIN);

                        //To detect methods in the code we first need to extract class in the source code
                        logFunctionsStart(ktClasses, virtualFile);

                        analyzeAndroidAPIsInKotlinFiles(virtualFile);

                        /*
                         * IMPORTANT NOTE: Missing imports should be added after log statements; otherwise, we will get error that we try to change the un-commited document
                         * Add the import log statement if it is not exist
                         * */
                        if (!importLogStatementAvailable) {
                            importChecker.addLogImportStatement(psiFile,LANGUAGE_KOTLIN);
                        }
                    }
                } else {
                    System.out.println("[GreenMeter -> actionPerformed$ Fatal Error: unknown file type: " + virtualFile.getName().trim());
                }
            }
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

    // UI - Getting Input
    private void getBatteryHealthAndCapacity() {
        BatteryHealthAndCapacityDialog dialog = new BatteryHealthAndCapacityDialog();
        if (dialog.showAndGet()) { // showAndGet() returns true if OK is clicked
            String batteryCapacity = dialog.getBatteryCapacity();
            String batteryHealth = dialog.getBatteryHealth();

            if (batteryCapacity == null && batteryHealth == null) {
                Messages.showWarningDialog("No values were inserted. Default values will be used! \n Battery capacity = 5000 mAh, Battery health = 100%", "Battery Capacity and Health Status");
            } else if (batteryCapacity == null && batteryHealth != null) {
                Messages.showWarningDialog("No values is inserted for battery capacity. Default value will be used! \n Battery capacity = 5000 mAh", "Battery Capacity and Health Status");
            } else if (batteryCapacity != null && batteryHealth == null) {
                Messages.showWarningDialog("No values is inserted for battery health. Default value will be used! \n Battery health = 100%", "Battery Capacity and Health Status");
            }else {
                if (isValidDouble(batteryCapacity) && isValidDouble(batteryHealth)) {
                    double batteryCapacityDouble = Double.parseDouble(batteryCapacity);
                    PowerXML.setBatteryCapacity(batteryCapacityDouble);

                    double batteryHealthDouble = Double.parseDouble(batteryHealth);
                    if (0 <= batteryHealthDouble &&  batteryHealthDouble <= 100) {
                        PowerXML.setStateOfHealth(batteryHealthDouble);
                    }else {
                        Messages.showWarningDialog("The value of battery health is out of range. Default value will be used! \n Battery capacity = 100%", "Battery Capacity and Health Status");
                    }

                }else if (!isValidDouble(batteryCapacity) && isValidDouble(batteryHealth)) {
                    Messages.showWarningDialog("The value of battery capacity is not in the correct format. Default value will be used! \n Battery capacity = 5000 mAh", "Battery Capacity and Health Status");
                }else if (isValidDouble(batteryCapacity) && !isValidDouble(batteryHealth)) {
                    Messages.showWarningDialog("The value of battery health is not in the correct format. Default value will be used! \n Battery health = 100%", "Battery Capacity and Health Status");
                }else {
                    Messages.showWarningDialog("The values are not in the correct format. Default value will be used! \n Battery capacity = 5000 mAh, Battery health = 100%", "Battery Capacity and Health Status");
                }
            }
        }
    }
    // UI - Getting Input
    private boolean isValidDouble(String value){
        if (value == null || value.length() == 0) {
            return false;
        }

        try {
            Double.parseDouble(value);
            return true;
        }catch (NumberFormatException e){
            return false;
        }
    }
    // UI - Getting Input
    private void showPowerXmlFileChooser() {
        // Create a FileChooserDescriptor to specify what kind of files to allow
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        fileChooserDescriptor.setTitle("Select a Txt File");
        fileChooserDescriptor.setDescription("Select the power txt file exported from a mobile device.");

        // Open the file chooser
        VirtualFile file = FileChooser.chooseFile(fileChooserDescriptor, project, null);

        if (file != null) {
            // Perform actions with the selected file
            Messages.showInfoMessage("You have selected: " + file.getPath(), "File Selected");
        } else {
            Messages.showWarningDialog("No file was selected. Default values will be used for hardware components power usage!", "File Not Selected");
        }
    }


    // This method converts VirtualFiles to psiClass (Java classes)
    private static PsiClass[] convertVirtualFileToPsiClass(Project project, VirtualFile virtualFile) {
        if (virtualFile == null) {
            System.out.println("[GreenMeter -> actionPerformed -> convertVirtualFileToPsiClass$ Fatal error: VirtualFile is null");
            return null;
        }

        //We considered only java and kotlin files, so we do not need this
//        if (!virtualFile.getName().endsWith(".java")) {
//            System.out.println("[GreenMeter -> actionPerformed -> convertVirtualFileToPsiClass$ Fatal error: VirtualFile is not Java file type");
//            return null;
//        }

        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile psiFile = psiManager.findFile(virtualFile);

        if(psiFile == null) {
            System.out.println("[GreenMeter -> actionPerformed -> convertVirtualFileToPsiClass$ Fatal error: The psiFile related to virtualFile could not be found.");
            return null;
        }

        PsiClass[] classes;
        if (psiFile instanceof PsiJavaFile) {
            PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
            classes = psiJavaFile.getClasses();
            if (classes.length == 0) {
                System.out.println("[GreenMeter -> actionPerformed -> convertVirtualFileToPsiClass $ Fatal error: There is no class associated to the input virtual file.");
                return null;
            }
            return classes;
//        }
//        else if (psiFile instanceof KtFile) {
//            KtFile psiKtFile = (KtFile) psiFile;
//            classes = psiKtFile.getClasses();
//            if (classes.length == 0) {
//                System.out.println("[GreenMeter -> actionPerformed -> convertVirtualFileToPsiClass $ Fatal error: There is no class associated to the input virtual file.");
//                return null;
//            }
//            return classes;
        }else {
            System.out.println("[GreenMeter -> actionPerformed -> convertVirtualFileToPsiClass $ Fatal error: There is no class associated to the input virtual file.");
            System.out.println("[GreenMeter -> actionPerformed -> convertVirtualFileToPsiClass $ Fatal error: The file is neither Java nor Kotlin.");
            return null;
        }
    }

    // This method converts VirtualFiles to psiClass (Kotlin classes)
    private static List<KtClass> convertVirtualFileToKotlinClass(Project project, VirtualFile virtualFile) {
        if (virtualFile == null) {
            System.out.println("[GreenMeter -> actionPerformed -> convertVirtualFileToKotlinClass$ Fatal error: VirtualFile is null");
            return null;
        }

        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile psiFile = psiManager.findFile(virtualFile);
        if(psiFile == null) {
            System.out.println("[GreenMeter -> actionPerformed -> convertVirtualFileToKotlinClass$ Fatal error: The psiFile related to virtualFile could not be found.");
            return null;
        }

        KtFile psiKtFile = (KtFile) psiFile;
        if (psiKtFile == null) {
            System.out.println("[GreenMeter -> actionPerformed -> convertVirtualFileToKotlinClass$ Fatal error: The psiKtFile related to virtualFile could not be found.");
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
            System.out.println("[GreenMeter -> actionPerformed -> convertVirtualFileToKotlinClass$ Fatal error: There is no class associated to the input virtual file.");
            return null;
        }
        return kotlinClasses;
    }

    private void logMethodsStart(PsiClass[] psiClasses, VirtualFile virtualFile) {

        //Extracts the class from the classes array
        for (PsiClass psiClass : psiClasses) {
            String className = psiClass.getName();
            System.out.println("[GreenMeter -> actionPerformed -> logMethodsStart$ The class name is " + className);

            //Get all the methods in the class
            psiMethods = psiClass.getMethods();

            if (psiMethods != null) {
                for (PsiMethod psiMethod : psiMethods) {
                    String methodName = psiMethod.getName();
                    System.out.println("[GreenMeter -> actionPerformed -> logMethodsStart$ The method name is " + methodName);

                    //Get method body
                    PsiCodeBlock methodBody = psiMethod.getBody();

                    if (methodBody == null) {
                        System.out.println("[GreenMeter -> actionPerformed -> logMethodsStart$ The " + methodName + " method is empty so it will not consume energy!");
                    }else {

                        //Extract Android API calls from the method body
                        retrieveAPICallsInMethod(className, methodName, methodBody);

                        //Generate the method start log statement
                        String startLogStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + methodName + "," + className + "," + MethodStart_TAG + ")\");";
                        PsiStatement startLogStatementElement = factory.createStatementFromText(startLogStatement,psiMethod);

                        //Add the method start log statement
                        WriteCommandAction.runWriteCommandAction(project, (Runnable) () -> {methodBody.addBefore(startLogStatementElement, methodBody.getFirstBodyElement());});

                        PsiStatement[] statements = methodBody.getStatements();
                        // Check if the code block has at least one statement
                        if(statements.length > 0) {
                            PsiStatement lastStatement = statements[statements.length - 1]; // Get the last statement

                            // Check if the last statement is a method call to finish()
                            if (lastStatement instanceof PsiExpressionStatement) {
                                PsiExpression expression = ((PsiExpressionStatement) lastStatement).getExpression();
                                if (expression instanceof PsiMethodCallExpression) {
                                    PsiMethodCallExpression methodCall = (PsiMethodCallExpression) expression;
                                    PsiReferenceExpression methodExpression = methodCall.getMethodExpression();
                                    String lastMethodName = methodExpression.getReferenceName();

                                    //if (!"finish".equals(lastMethodName) && !"startActivityForResult".equals(lastMethodName)) {
                                    if (!"finish".equals(lastMethodName)) {

                                        generateMethodEndLogAndAdd(methodName, className, psiMethod, methodBody);

                                    } else {

                                        //Generate the method end log statement
                                        String endLogStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + methodName + "," + className + "," + MethodEnd_TAG + ")\");";
                                        PsiStatement endLogStatementElement = factory.createStatementFromText(endLogStatement, psiMethod);

                                        // Insert the log statement before the finish() call
                                        WriteCommandAction.runWriteCommandAction(project, () -> {
                                            methodBody.addBefore(endLogStatementElement, lastStatement);
                                        });
                                    }
                                }
                            }else if (lastStatement instanceof PsiReturnStatement){
                                //Generate the method end log statement
                                String endLogStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + methodName + "," + className + "," + MethodEnd_TAG + ")\");";
                                PsiStatement endLogStatementElement = factory.createStatementFromText(endLogStatement, psiMethod);

                                // Insert the log statement before the finish() call
                                WriteCommandAction.runWriteCommandAction(project, () -> {
                                    methodBody.addBefore(endLogStatementElement, lastStatement);
                                });
                            }else {
                                generateMethodEndLogAndAdd(methodName, className, psiMethod, methodBody);
                            }
                        }else {
                            generateMethodEndLogAndAdd(methodName, className, psiMethod, methodBody);

                        }

                    }
                }
            }else {
                System.out.println("[GreenMeter -> actionPerformed -> logMethodsStart$ The class named " + className + " is empty and does not have any methods!");
            }
        }
    }


    private void logFunctionsStart(List<KtClass> kotlinClasses, VirtualFile virtualFile) {

        //Extracts the class from the classes list
        for (KtClass kotlinClass : kotlinClasses) {
            String className = kotlinClass.getName();
            System.out.println("[GreenMeter -> actionPerformed -> logFunctionsStart$ The class name is " + className);

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
                    String functionName = function.getName();
                    System.out.println("[GreenMeter -> actionPerformed -> logFunctionsStart$ The function name is " + functionName);

                    KtExpression functionBody = function.getBodyExpression();
                    //KtBlockExpression functionBody = function.getBodyBlockExpression();

                    if (functionBody == null) {
                        System.out.println("[GreenMeter -> actionPerformed -> logFunctionsStart$ The class named " + className + " is empty and does not have any methods!");
                    }else {
                        //Extract Android API calls from the method body
                        retrieveAPICallsInKotlinFunction(className, functionName, functionBody);

                        //Generate the method start log statement
                        String startLogStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + functionName + "," + className + "," + MethodStart_TAG + ")\")";
                        KtExpression expression = factoryKotlin.createExpression(startLogStatement);
                        //KtStatementExpression expression = (KtStatementExpression) factoryKotlin.createExpression(startLogStatement);

                        //Add the method start log statement
                        WriteCommandAction.runWriteCommandAction(project, (Runnable) () -> {
                            PsiElement firstContent = functionBody.getFirstChild();

                            if (firstContent != null) {
                                functionBody.addAfter(expression,functionBody.getFirstChild());
                            }else {
                                functionBody.add(expression);
                            }
                        });

                        KtBlockExpression functionBodyBlock = function.getBodyBlockExpression();
                        List<KtExpression> ktExpressions = functionBodyBlock.getStatements();

                        // Check if the code block has at least one statement
                        if(ktExpressions.size() > 0) {
                            KtExpression lastStatement = ktExpressions.getLast(); // Get the last statement

                            // Check if the last statement is a function call to finish()
                            if (lastStatement instanceof KtCallExpression) {

                                KtCallExpression lastFunctionCall = (KtCallExpression) lastStatement;

                                // Get the method name
                                KtExpression calleeExpression = lastFunctionCall.getCalleeExpression();
                                String lastFunctionCallName = (calleeExpression != null) ? calleeExpression.getText() : null;

                                if (!"finish".equals(lastFunctionCallName)) { // if the last statement is a function call but not a finish()

                                    generateFunctionEndLogAndAdd(functionName, className, function);

                                } else { // if the last statement is finish()

                                    //Generate the method end log statement
                                    String endLogStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + functionName + "," + className + "," + MethodEnd_TAG + ")\")";
                                    KtExpression endLogStatementElement = factoryKotlin.createExpression(endLogStatement);

                                    // Insert the log statement before the finish() call
                                    WriteCommandAction.runWriteCommandAction(project, () -> {

                                        // Insert the end log statement after the newline
                                        functionBody.addBefore(endLogStatementElement, lastStatement);

                                        // Create a newline (white space) element
                                        PsiElement newLine = factoryKotlin.createNewLine();

                                        // Insert the newline before the last statement
                                        functionBody.addBefore(newLine, lastStatement);

                                    });
                                }

                            }else if (lastStatement instanceof KtReturnExpression){ // if the last statement is return
                                //Generate the method end log statement
                                String endLogStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + functionName + "," + className + "," + MethodEnd_TAG + ")\")";
                                KtExpression endLogStatementElement = factoryKotlin.createExpression(endLogStatement);

                                // Insert the log statement before the finish() call
                                WriteCommandAction.runWriteCommandAction(project, () -> {

                                    // Insert the end log statement after the newline
                                    functionBody.addBefore(endLogStatementElement, lastStatement);

                                    // Create a newline (white space) element
                                    PsiElement newLine = factoryKotlin.createNewLine();

                                    // Insert the newline before the last statement
                                    functionBody.addBefore(newLine, lastStatement);

                                });
                            }else { // all other type of expressions -> add the statement
                                generateFunctionEndLogAndAdd(functionName, className, function);
                            }

                        }else { // function body is empty -> add the statement
                            generateFunctionEndLogAndAdd(functionName, className, function);

                        }
                    }
                }
            } else {
                System.out.println("[GreenMeter -> actionPerformed -> logFunctionsStart$ There is not any functions in the class " + className + "!");
            }
        }
    }


    private void generateMethodEndLogAndAdd(String methodName, String className, PsiMethod psiMethod, PsiCodeBlock methodBody) {
        //Generate the method end log statement
        String endLogStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + methodName + "," + className + "," + MethodEnd_TAG + ")\");";
        PsiStatement endLogStatementElement = factory.createStatementFromText(endLogStatement, psiMethod);

        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);


        //Add the method end log statement
        WriteCommandAction.runWriteCommandAction(project, (Runnable) () -> {
            methodBody.add(endLogStatementElement);
        });
    }


    private void generateFunctionEndLogAndAdd(String functionName, String className, KtNamedFunction function) {
        KtBlockExpression functionBodyBlock = function.getBodyBlockExpression();
        List<KtExpression> ktExpressions = functionBodyBlock.getStatements();

        KtExpression lastStatement = ktExpressions.getLast();

        //Generate the method end log statement
        String endLogStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + functionName + "," + className + "," + MethodEnd_TAG + ")\")";
        KtExpression endLogStatementElement = factoryKotlin.createExpression(endLogStatement);

        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);

        // Insert the log statement before the finish() call
        WriteCommandAction.runWriteCommandAction(project, () -> {
            functionBodyBlock.addBefore(endLogStatementElement, functionBodyBlock.getLastChild());
        });
    }


    // Extract API calls and count them within the input kotlin function body
    private void retrieveAPICallsInKotlinFunction(String functionClassName, String inputFunctionName, KtExpression ktExpression) {
        List<KtCallExpression> functionCalls = new ArrayList<>();

        if (ktExpression != null) {
            // Traverse the body for function calls
            PsiTreeUtil.processElements(ktExpression, element -> {
                if (element instanceof KtCallExpression) {
                    functionCalls.add((KtCallExpression) element);
                }
                return true; // Continue traversal
            });


            for (KtCallExpression callExpression : functionCalls) {
//                KtCallExpression functionCall = (KtCallExpression) callExpression;
//                String functionCallName =  ((KtCallExpression) callExpression).getName();

                String functionCallNameWithArguments =  callExpression.getText(); // Get the function call with its all arguments
                String functionCallName = functionCallNameWithArguments.substring(0, functionCallNameWithArguments.indexOf("(")); // Get the function call name without its arguments

                System.out.println("----------- DroidEC -> Function call found: " + functionCallName);

                if (singleton.redAPICalls.keySet().contains(functionCallName)) {
                    if (!functionCallName.equals("d")) {
                        updateMethodsEnergyMaps(functionCallName, functionClassName, inputFunctionName);
                    } else {

                        if (!functionCallNameWithArguments.contains(Logging_TAG)) {
                            updateMethodsEnergyMaps(functionCallName, functionClassName, inputFunctionName);
                        }
                    }
                }
            }
        }

        // Copy the methodsAPICallsCountLocalMap to a Map in the singleton, so all the classes can access it
        if (!methodsAPICallsCountLocalMap.isEmpty()) {
            singleton.fillMethodsAPICallsCountMap(methodsAPICallsCountLocalMap);
        }

        if (!methodsAPICallsTotalEnergyCostLocalMap.isEmpty()) {
            singleton.fillMethodsAPICallsEnergyMap(methodsAPICallsTotalEnergyCostLocalMap);
        }
    }

    // Extract API calls and count them within the input method body
    public void retrieveAPICallsInMethod(String methodClassName, String inputMethodName, PsiCodeBlock methodBody) {
        List<PsiMethodCallExpression> methodCalls = new ArrayList<>();

        methodCalls.addAll(PsiTreeUtil.collectElementsOfType(methodBody, PsiMethodCallExpression.class));

        //TODO: Correct the API calls energy
        for (PsiMethodCallExpression psiMethodCallExpression : methodCalls) {
            PsiReferenceExpression methodExpression = psiMethodCallExpression.getMethodExpression();
            String methodName = methodExpression.getReferenceName();  // Get the method name
            System.out.println("----------- DroidEC -> Method call found: " + methodName);

            if (singleton.redAPICalls.keySet().contains(methodName)){
                if (!methodName.equals("d")) {
                    updateMethodsEnergyMaps(methodName, methodClassName, inputMethodName);
                } else {
                    PsiExpressionList argumentList = psiMethodCallExpression.getArgumentList();
                    if (argumentList != null) {
                        PsiExpression[] arguments = argumentList.getExpressions();
                        PsiExpression firstArgument = arguments[0];
                        if (!firstArgument.getText().contains(Logging_TAG)) {
                            updateMethodsEnergyMaps(methodName, methodClassName, inputMethodName);
                        }
                    }else{
                        updateMethodsEnergyMaps(methodName, methodClassName, inputMethodName);
                    }
                }
            }



//            if (methodsAPICallsCountLocalMap.isEmpty()){ // If this is true, it means that this is the first item we are putting in the Map, so easily add
//                // If the method call is red API call, it affects the energy consumption and should be logged
//                if (singleton.redAPICalls.keySet().contains(methodName)) {
//                    methodsAPICallsCountLocalMap.put(new ThreeStringKey(methodClassName,inputMethodName,methodName),1);
//                    methodsAPICallsTotalEnergyCostLocalMap.put(new TwoStringKey(methodClassName,inputMethodName),singleton.redAPICalls.get(methodName).doubleValue());
//                }
//            }else{
//                // If the method call is red API call, it affects the energy consumption and should be logged
//                if (singleton.redAPICalls.keySet().contains(methodName)) {
//
//                    ThreeStringKey key = new ThreeStringKey(methodClassName,inputMethodName,methodName);
//                    TwoStringKey twoStringKey = new TwoStringKey(methodClassName,inputMethodName);
//                    // The key is already exist
//                    if (methodsAPICallsCountLocalMap.containsKey(key)) {
//                        int oldValue = methodsAPICallsCountLocalMap.get(key);
//                        methodsAPICallsCountLocalMap.put(key,oldValue+1);
//
//                        double oldEnergyCost = methodsAPICallsTotalEnergyCostLocalMap.get(twoStringKey).doubleValue();
//                        methodsAPICallsTotalEnergyCostLocalMap.put(twoStringKey,oldEnergyCost+singleton.redAPICalls.get(methodName).doubleValue());
//                    }else {
//                        // First time to add the key
//                        methodsAPICallsCountLocalMap.put(key,1);
//                        methodsAPICallsTotalEnergyCostLocalMap.put(twoStringKey,singleton.redAPICalls.get(methodName).doubleValue());
//                    }
//                }
//            }

        }

        // Copy the methodsAPICallsCountLocalMap to a Map in the singleton, so all the classes can access it
        if (!methodsAPICallsCountLocalMap.isEmpty()) {
            singleton.fillMethodsAPICallsCountMap(methodsAPICallsCountLocalMap);
        }

        if (!methodsAPICallsTotalEnergyCostLocalMap.isEmpty()) {
            singleton.fillMethodsAPICallsEnergyMap(methodsAPICallsTotalEnergyCostLocalMap);
        }
    }


//    public void copyMethodInfo(){
//        // Copy the methodsAPICallsCountLocalMap to a Map in the singleton, so all the classes can access it
//        if (!methodsAPICallsCountLocalMap.isEmpty()) {
//            singleton.fillMethodsAPICallsCountMap(methodsAPICallsCountLocalMap);
//        }
//
//        if (!methodsAPICallsTotalEnergyCostLocalMap.isEmpty()) {
//            singleton.fillMethodsAPICallsEnergyMap(methodsAPICallsTotalEnergyCostLocalMap);
//        }
//    }


    private void updateMethodsEnergyMaps(String methodName, String methodClassName, String inputMethodName) {

        if (methodsAPICallsCountLocalMap.isEmpty()){ // If this is true, it means that this is the first item we are putting in the Map, so easily add
            // If the method call is red API call, it affects the energy consumption and should be logged
            if (singleton.redAPICalls.keySet().contains(methodName)) {
                methodsAPICallsCountLocalMap.put(new ThreeStringKey(methodClassName,inputMethodName,methodName),1);
                methodsAPICallsTotalEnergyCostLocalMap.put(new TwoStringKey(methodClassName,inputMethodName),singleton.redAPICalls.get(methodName).doubleValue());
            }
        }else{
            // If the method call is red API call, it affects the energy consumption and should be logged
            if (singleton.redAPICalls.keySet().contains(methodName)) {

                ThreeStringKey key = new ThreeStringKey(methodClassName,inputMethodName,methodName);
                TwoStringKey twoStringKey = new TwoStringKey(methodClassName,inputMethodName);
                // The key is already exist
                if (methodsAPICallsCountLocalMap.containsKey(key)) {
                    int oldValue = methodsAPICallsCountLocalMap.get(key);
                    methodsAPICallsCountLocalMap.put(key,oldValue+1);

                    double oldEnergyCost = methodsAPICallsTotalEnergyCostLocalMap.get(twoStringKey).doubleValue();
                    methodsAPICallsTotalEnergyCostLocalMap.put(twoStringKey,oldEnergyCost+singleton.redAPICalls.get(methodName).doubleValue());
                }else {
                    // First time to add the key
                    methodsAPICallsCountLocalMap.put(key,1);
                    methodsAPICallsTotalEnergyCostLocalMap.put(twoStringKey,singleton.redAPICalls.get(methodName).doubleValue());
                }
            }
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

    // This method travers the input Java virtual file and finds method call
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

                    }else{
                        //TODO: for extending the work
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

    // This method travers the input Kotlin virtual file and finds method call
    // and filters specific API calls and adds the proper log statements
    private void analyzeAndroidAPIsInKotlinFiles(VirtualFile inputVirtualFile) {
        PsiFile inputPsiFile = psiManager.findFile(inputVirtualFile);

        if (inputPsiFile != null) {
            System.out.println("[GreenMeter -> analyzeAndroidAPIsInKotlinFiles$ psiFile name is " + inputPsiFile.getName());

            inputPsiFile.accept(new KtTreeVisitorVoid() {
                @Override
                public void visitCallExpression(@NotNull KtCallExpression callExpression) {
                    super.visitCallExpression(callExpression);

                    String fileName = inputPsiFile.getName();
                    KtExpression calleeExpression = callExpression.getCalleeExpression();

                    // Get the function call name
                    String functionCallName = (calleeExpression != null) ? calleeExpression.getText() : null;
                    System.out.println("[GreenMeter -> analyzeAndroidAPIsInKotlinFiles$ methodCallName is " + functionCallName);

                    String fullQualifiedMethodName = null;
                    if (callExpression != null) {
                        PsiElement parentExpression = callExpression.getParent();
                        if (parentExpression instanceof KtDotQualifiedExpression) {
                            // Cast the parent to KtDotQualifiedExpression
                            KtDotQualifiedExpression qualifiedExpression = (KtDotQualifiedExpression) parentExpression;

                            // Get the receiver expression (e.g., "Log")
                            String qualifier = qualifiedExpression.getReceiverExpression().getText();

                            // Combine the receiver expression and function name to form e.g., "Log.d"
                            fullQualifiedMethodName = qualifier + "." + functionCallName;
                        } else {
                            fullQualifiedMethodName = functionCallName;
                        }
                    }

                    System.out.println("[GreenMeter -> analyzeAndroidAPIsInKotlinFiles$ methodCallName parent name is " + fullQualifiedMethodName);

                    // Check for unique APIs
                    if (singleton.redAPICalls.containsKey(functionCallName)) {
                        if (!"d".equals(functionCallName)) {
                            addLogStatementToKotlinFile(callExpression, functionCallName, fileName);
                        } else {
                            KtValueArgumentList argumentList = callExpression.getValueArgumentList();
                            if (argumentList != null) {
                                List<KtValueArgument> arguments = argumentList.getArguments();
                                if (!arguments.isEmpty()) {
                                    KtValueArgument firstArgument = arguments.get(0);
                                    if (!firstArgument.getText().contains(Logging_TAG)) {
                                        addLogStatementToKotlinFile(callExpression, functionCallName, fileName);
                                    }
                                } else {
                                    addLogStatementToKotlinFile(callExpression, functionCallName, fileName);
                                }
                            }
                        }
                    } else {
                        // Extend work for other API call types
                        if (singleton.hwAPICalls.containsKey(fullQualifiedMethodName)) {
                            // Case 2: hardware APIs
                        } else if (singleton.jointRedAPICalls.containsKey(fullQualifiedMethodName)) {
                            // Case 3: repetitive APIs
                        } else {
                            // Case 4: CPU/Memory intensive
                        }
                    }
                }
            });


        }else {
            System.out.println("[GreenMeter -> analyzeAndroidAPIsInKotlinFiles$ There is not equivalent PSI file for input virtual file.");
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
            if (!methodCallName.equals("finish") && !methodCallName.equals("startActivityForResult")) {

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
                //Log statement should be added before finish, startActivityForResult, and .... MAYBE FOUND IN THE FUTURE

                int lineNumber; // Holds the exact line number of the API call
                if (importLogStatementAvailable){
                    lineNumber = getLineNumber(parent) + 2;
                }else {
                    lineNumber = getLineNumber(parent) + 4;
                }

                String logStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + methodCallName + "," + javaFile + "," + lineNumber + ")\");";
                PsiStatement logStatementElement = factory.createStatementFromText(logStatement,expression.getContext());


                PsiElement target = parent.getParent();
                PsiElement parentElement = parent;
                WriteCommandAction.runWriteCommandAction(project, (Runnable) () -> {target.addBefore(logStatementElement,parentElement);});

            }

        }else{
            System.out.println("[GreenMeter -> logFindViewById$ Fatal error: Method call expression is null: There is not any method call!");
        }

    }

    //This method creates and adds the log statements to the source code of the application
    private void addLogStatementToKotlinFile(KtCallExpression expression, String functionCallName, String kotlinFile) {

        System.out.println("[GreenMeter -> addLogStatementToKotlinFile$ addLogStatementToKotlinFile method is called");

        /*
        * Traverse upwards if the parent is a KtDotQualifiedExpression to get the full expression
        * Check if the call expression is part of a chain
        * This block code is to get the full expression of the function call
        * The reason that we used parentLookedUp variable is the requirement of
        * a new line if the parent traversed more than one time
        *  */
        PsiElement parent = expression.getParent();

        int parentLookedUp = 0;
        PsiElement previousParent = parent;
        while (!(parent instanceof KtBlockExpression) || parent == null) {
            parentLookedUp++;
            previousParent = parent;
            parent = parent.getParent();
        }
        parent = previousParent;


        if (parent != null || parent.isValid()) {

            int lineNumber;
            if (!"finish".equals(functionCallName) && !"startActivityForResult".equals(functionCallName)) {
                // Line number for log statement
                if (importLogStatementAvailable) {
                    lineNumber = getLineNumber(expression) + 1;
                } else {
                    lineNumber = getLineNumber(expression) + 3;
                }

                // Create log statement using KtPsiFactory
                KtPsiFactory factory = new KtPsiFactory(project);
                String logStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + functionCallName + "," + kotlinFile + "," + lineNumber + ")\")";
                KtExpression logStatementElement = factory.createExpression(logStatement);

                PsiElement finalParent = parent;
                KtCallExpression finalExpression = expression;
                int finalParentLookedUp = parentLookedUp;
                WriteCommandAction.runWriteCommandAction(project, () -> {

                    if (finalParentLookedUp > 1){
                        PsiElement newLine = factoryKotlin.createNewLine();
                        finalParent.addAfter(newLine, finalExpression);
                    }

                    PsiElement addedElement = finalParent.addAfter(logStatementElement, finalExpression);

                    // Optional: Add an actual new line for readability
                    PsiElement newLine = factoryKotlin.createNewLine();
                    finalParent.addAfter(newLine, finalExpression);

                    // Reformat the added element for proper indentation
                    CodeStyleManager.getInstance(project).reformat(addedElement);
                });


            } else {
                // Handle special cases for "finish", "startActivityForResult"
                if (importLogStatementAvailable) {
                    lineNumber = getLineNumber(expression) + 2;
                } else {
                    lineNumber = getLineNumber(expression) + 4;
                }

                // Create log statement
                KtPsiFactory factory = new KtPsiFactory(project);
                String logStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + functionCallName + "," + kotlinFile + "," + lineNumber + ")\")";
                KtExpression logStatementElement = factory.createExpression(logStatement);

                // Insert log statement before the target expression
                PsiElement finalParent1 = parent;
                KtCallExpression finalExpression1 = expression;
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    finalParent1.addBefore(logStatementElement, finalExpression1);

                    // Optional: Add an actual new line for readability
                    PsiElement newLine = factoryKotlin.createNewLine();
                    finalParent1.addBefore(newLine, finalExpression1);
                });
            }

        }else{
            System.out.println("[GreenMeter -> addLogStatementToKotlinFile$ Fatal Error: Function call expression is null: There is no function call!");
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
