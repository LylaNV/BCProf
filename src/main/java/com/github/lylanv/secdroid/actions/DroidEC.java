package com.github.lylanv.secdroid.actions;

import com.github.lylanv.secdroid.inspections.BatteryHealthAndCapacityDialog;
import com.github.lylanv.secdroid.inspections.ImportChecker;
import com.github.lylanv.secdroid.inspections.PowerXML;
import com.github.lylanv.secdroid.inspections.Singleton;
import com.github.lylanv.secdroid.utils.ThreeStringKey;
import com.github.lylanv.secdroid.utils.TwoStringKey;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.psi.*;

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
    private final String Logging_TAG = "BPDroid"; //A TAG that we use in adding logs, so we can differentiate our added logs from rest of logs
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

    //-----------------------------------------------------------------------------
    private void logMethodsStart(PsiClass[] psiClasses, VirtualFile virtualFile) {
        for (PsiClass psiClass : psiClasses) {
            String className = psiClass.getName();
            psiMethods = psiClass.getMethods();
            if (psiMethods == null) continue;

            for (PsiMethod psiMethod : psiMethods) {
                String methodName = psiMethod.getName();
                PsiCodeBlock methodBody = psiMethod.getBody();
                if (methodBody == null) continue;

                retrieveAPICallsInMethod(className, methodName, methodBody);

                // Add START log
                String startLog = "Log.d(\"" + Logging_TAG + "\", \"(" + methodName + "," + className + "," + MethodStart_TAG + ")\");";
                PsiStatement startLogElement = factory.createStatementFromText(startLog, psiMethod);
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    PsiStatement[] statements = methodBody.getStatements();
                    if (psiMethod.isConstructor() && statements.length > 0) {
                        String text = statements[0].getText();
                        if (text.startsWith("super(") || text.startsWith("this(")) {
                            methodBody.addAfter(startLogElement, statements[0]);
                            return;
                        }
                    }
                    methodBody.addBefore(startLogElement, methodBody.getFirstBodyElement());
                });

                // Track returns and check if the very last top-level statement blocks the end brace
                List<PsiReturnStatement> returnStatements = new ArrayList<>();
                List<PsiStatement> exitStatements = new ArrayList<>();
                boolean endsWithUnconditionalExit = false;

                PsiStatement[] finalStatements = methodBody.getStatements();
                if (finalStatements.length > 0) {
                    PsiStatement lastTopLevelStatement = finalStatements[finalStatements.length - 1];
                    endsWithUnconditionalExit = isStatementTerminal(lastTopLevelStatement);
                }

                // Find all exits (returns and throw exceptions) (including deeply nested ones) to inject END logs before them
                methodBody.accept(new JavaRecursiveElementWalkingVisitor() {
                    @Override
                    public void visitReturnStatement(@NotNull PsiReturnStatement statement) {
                        super.visitReturnStatement(statement);
                        returnStatements.add(statement);
                        exitStatements.add(statement);
                    }

                    @Override
                    public void visitThrowStatement(@NotNull PsiThrowStatement statement) {
                        super.visitThrowStatement(statement);
                        exitStatements.add(statement);
                    }
                });

                // Insert END logs before all found returns
                // for (PsiReturnStatement returnStmt : returnStatements) {
                for (PsiStatement exitStmt : exitStatements) {
                    String endLog = "Log.d(\"" + Logging_TAG + "\", \"(" + methodName + "," + className + "," + MethodEnd_TAG + ")\");";

                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        PsiElement exitParent = exitStmt.getParent();

                        // 1. Check if the exit sits directly inside a braceless If Statement
                        if (exitParent instanceof PsiIfStatement) {
                            PsiIfStatement ifStmt = (PsiIfStatement) exitParent;
                            String wrappedBlockText = "{\n" + endLog + "\n" + exitStmt.getText() + "\n}";
                            PsiCodeBlock newBlock = JavaPsiFacade.getElementFactory(project)
                                    .createCodeBlockFromText(wrappedBlockText, exitStmt);

                            // Explicitly determine which branch child to re-route safely
                            if (ifStmt.getThenBranch() == exitStmt) {
                                ifStmt.getThenBranch().replace(newBlock);
                            } else if (ifStmt.getElseBranch() == exitStmt) {
                                ifStmt.getElseBranch().replace(newBlock);
                            }
                        }
                        // 2. Check if the exit sits directly inside a braceless Loop Statement
                        else if (exitParent instanceof PsiLoopStatement) {
                            PsiLoopStatement loopStmt = (PsiLoopStatement) exitParent;
                            String wrappedBlockText = "{\n" + endLog + "\n" + exitStmt.getText() + "\n}";
                            PsiCodeBlock newBlock = JavaPsiFacade.getElementFactory(project)
                                    .createCodeBlockFromText(wrappedBlockText, exitStmt);

                            if (loopStmt.getBody() == exitStmt) {
                                loopStmt.getBody().replace(newBlock);
                            }
                        }
                        // 3. Standard safe block scenario (already has curly braces)
                        else {
                            PsiStatement endLogElement = factory.createStatementFromText(endLog, psiMethod);
                            exitParent.addBefore(endLogElement, exitStmt);
                        }
                    });
                }

                // ONLY add a log at the very end of the method if it doesn't structurally end with a return/throw
                if (!endsWithUnconditionalExit) {
                    generateMethodEndLogAndAdd(methodName, className, psiMethod, methodBody);
                }
            }
        }
    }


    private boolean checkIfStatementExitsDefinitively(PsiIfStatement ifStatement) {
        PsiStatement thenBranch = ifStatement.getThenBranch();
        PsiStatement elseBranch = ifStatement.getElseBranch();

        // If there is no 'else' branch, execution can always skip the 'if' block and reach the bottom brace
        if (thenBranch == null || elseBranch == null) {
            return false;
        }

        return isStatementTerminal(thenBranch) && isStatementTerminal(elseBranch);
    }


    private boolean isStatementTerminal(PsiStatement statement) {
        if (statement == null) return false;

        // If the branch statement is wrapped in braces { ... }, look at its final internal statement
        if (statement instanceof PsiBlockStatement) {
            return isBlockTerminal(((PsiBlockStatement) statement).getCodeBlock());
        }

        // 1. Direct Return or Throw statements
        if (statement instanceof PsiReturnStatement || statement instanceof PsiThrowStatement) {
            return true;
        }
        // 2. If statements (must terminate cleanly in BOTH the then and else branches)
        else if (statement instanceof PsiIfStatement) {
            return checkIfStatementExitsDefinitively((PsiIfStatement) statement);
        }
        // 3. Try-Catch statements (terminal if both try and all catches terminate)
        else if (statement instanceof PsiTryStatement) {
            PsiTryStatement tryStmt = (PsiTryStatement) statement;

            // Check the try block body
            PsiCodeBlock tryBlock = tryStmt.getTryBlock();
            if (tryBlock == null || !isBlockTerminal(tryBlock)) {
                return false;
            }

            // Check every catch block body
            PsiCatchSection[] catchSections = tryStmt.getCatchSections();
            for (PsiCatchSection catchSection : catchSections) {
                PsiCodeBlock catchBlock = catchSection.getCatchBlock();
                if (catchBlock == null || !isBlockTerminal(catchBlock)) {
                    return false;
                }
            }

            return true;
        }

        // 4. Switch statement check
        else if (statement instanceof PsiSwitchStatement) {
            PsiSwitchStatement switchStmt = (PsiSwitchStatement) statement;
            PsiCodeBlock switchBody = switchStmt.getBody();
            if (switchBody == null) return false;

            PsiStatement[] statements = switchBody.getStatements();
            boolean hasDefault = false;

            // We need to inspect every block section inside the switch
            for (int i = 0; i < statements.length; i++) {
                if (statements[i] instanceof PsiSwitchLabelStatement) {
                    PsiSwitchLabelStatement label = (PsiSwitchLabelStatement) statements[i];
                    if (label.isDefaultCase()) {
                        hasDefault = true;
                    }

                    // Look ahead to find if this case block branch terminates
                    boolean branchTerminates = false;
                    for (int j = i + 1; j < statements.length; j++) {
                        // If we hit the next label or the end of the switch, check the statement right before it
                        if (statements[j] instanceof PsiSwitchLabelStatement) {
                            if (j > i + 1) {
                                branchTerminates = isStatementTerminal(statements[j - 1]);
                            }
                            break;
                        }
                        // If we hit the absolute end of the switch block statement array
                        if (j == statements.length - 1) {
                            branchTerminates = isStatementTerminal(statements[j]);
                        }
                    }

                    // If any branch doesn't return or throw (or uses a break statement), execution can escape
                    if (!branchTerminates) {
                        return false;
                    }
                }
            }

            // A switch is only fully terminal if it covers all bases with a default case
            return hasDefault;
        }

        return false;
    }


    // Helper to read code blocks safely
    private boolean isBlockTerminal(PsiCodeBlock block) {
        if (block == null) return false;
        PsiStatement[] statements = block.getStatements();
        if (statements.length == 0) return false;
        // Inspect the absolute final item in the code block sequence
        return isStatementTerminal(statements[statements.length - 1]);
    }
    //-----------------------------------------------------------------------------

    //-----------------------------------------------------------------------------
    private void logFunctionsStart(List<KtClass> kotlinClasses, VirtualFile virtualFile) {
        for (KtClass kotlinClass : kotlinClasses) {
            String className = kotlinClass.getName();
            List<KtNamedFunction> functions = new ArrayList<>();

            for (KtDeclaration declaration : kotlinClass.getDeclarations()) {
                if (declaration instanceof KtNamedFunction) {
                    functions.add((KtNamedFunction) declaration);
                }
            }

            for (KtNamedFunction function : functions) {
                String functionName = function.getName();
                KtExpression functionBody = function.getBodyExpression();
                if (functionBody == null) continue;

                retrieveAPICallsInKotlinFunction(className, functionName, functionBody);

                // Add START log
                String startLog = "Log.d(\"" + Logging_TAG + "\", \"(" + functionName + "," + className + "," + MethodStart_TAG + ")\")";
                KtExpression startLogElement = factoryKotlin.createExpression(startLog);
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    PsiElement firstContent = functionBody.getFirstChild();
                    if (firstContent != null) {
                        functionBody.addAfter(startLogElement, functionBody.getFirstChild());
                    } else {
                        functionBody.add(startLogElement);
                    }
                });

                // Check if the closing block area is dead code
                boolean endsWithUnconditionalExit = false;
                KtBlockExpression functionBodyBlock = function.getBodyBlockExpression();

                if (functionBodyBlock != null) {
                    List<KtExpression> ktExpressions = functionBodyBlock.getStatements();
                    if (!ktExpressions.isEmpty()) {
                        KtExpression lastTopLevelExpression = ktExpressions.get(ktExpressions.size() - 1);
                        endsWithUnconditionalExit = isKotlinExpressionTerminal(lastTopLevelExpression);
                    }
                }

                // Gather all exits (return and trow exception) expressions via visitor
                List<KtExpression> exitExpressions = new ArrayList<>();
                functionBody.accept(new KtTreeVisitorVoid() {
                    @Override
                    public void visitReturnExpression(@NotNull KtReturnExpression expression) {
                        super.visitReturnExpression(expression);
                        exitExpressions.add(expression);
                    }

                    @Override
                    public void visitThrowExpression(@NotNull KtThrowExpression expression) {
                        super.visitThrowExpression(expression);
                        exitExpressions.add(expression);
                    }
                });

                // Insert END logs before all returns
                for (KtExpression exitExpr : exitExpressions) {
                    String endLog = "Log.d(\"" + Logging_TAG + "\", \"(" + functionName + "," + className + "," + MethodEnd_TAG + ")\")";

                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        PsiElement exitParent = exitExpr.getParent();

                        // 1. Check if the exit sits directly inside a braceless Kotlin If Expression
                        if (exitParent instanceof KtIfExpression) {
                            KtIfExpression ifExpr = (KtIfExpression) exitParent;
                            String wrappedKotlinBlock = "{\n" + endLog + "\n" + exitExpr.getText() + "\n}";
                            KtExpression newKotlinBlock = factoryKotlin.createBlock(wrappedKotlinBlock);

                            if (ifExpr.getThen() == exitExpr) {
                                ifExpr.getThen().replace(newKotlinBlock);
                            } else if (ifExpr.getElse() == exitExpr) {
                                ifExpr.getElse().replace(newKotlinBlock);
                            }
                        }
                        // 2. Check if the exit sits inside a braceless Kotlin Loop or Container Node
                        else if (exitParent instanceof KtContainerNode || exitParent instanceof KtLoopExpression) {
                            String wrappedKotlinBlock = "{\n" + endLog + "\n" + exitExpr.getText() + "\n}";
                            KtExpression newKotlinBlock = factoryKotlin.createBlock(wrappedKotlinBlock);
                            exitExpr.replace(newKotlinBlock);
                        }
                        // 3. Standard safe block scenario (already has curly braces)
                        else {
                            KtExpression endLogElement = factoryKotlin.createExpression(endLog);
                            exitParent.addBefore(endLogElement, exitExpr);
                            exitParent.addBefore(factoryKotlin.createNewLine(), exitExpr);
                        }
                    });
                }

                // ONLY add to the trailing block body if the last expression isn't natively terminating the flow
                if (functionBodyBlock != null && !endsWithUnconditionalExit) {
                    generateFunctionEndLogAndAdd(functionName, className, function);
                }
            }
        }
    }


    private boolean checkKotlinIfExitsDefinitively(KtIfExpression ifExpression) {
        KtExpression thenBranch = ifExpression.getThen();
        KtExpression elseBranch = ifExpression.getElse();

        if (thenBranch == null || elseBranch == null) {
            return false;
        }

        return isKotlinExpressionTerminal(thenBranch) && isKotlinExpressionTerminal(elseBranch);
    }


    private boolean isKotlinExpressionTerminal(KtExpression expression) {
        if (expression == null) return false;

        // If the expression is an entire block { ... }, evaluate its final statement
        if (expression instanceof KtBlockExpression) {
            List<KtExpression> innerExpressions = ((KtBlockExpression) expression).getStatements();
            if (innerExpressions.isEmpty()) return false;
            expression = innerExpressions.get(innerExpressions.size() - 1);
        }

        // 1. Unconditional Local Exits
        if (expression instanceof KtReturnExpression || expression instanceof KtThrowExpression) {
            return true;
        }

        // 2. Conditional Branch Exits (Both branches must terminate)
        else if (expression instanceof KtIfExpression) {
            return checkKotlinIfExitsDefinitively((KtIfExpression) expression);
        }

        // 3. Try-Catch Expression Check
        else if (expression instanceof KtTryExpression) {
            KtTryExpression tryExpr = (KtTryExpression) expression;

            // A try expression is terminal only if its main block terminates...
            KtBlockExpression tryBlock = tryExpr.getTryBlock();
            if (tryBlock == null || !isKotlinBlockTerminal(tryBlock)) {
                return false;
            }

            // ...and EVERY SINGLE associated catch block also terminates.
            List<KtCatchClause> catchClauses = tryExpr.getCatchClauses();
            for (KtCatchClause clause : catchClauses) {
                KtExpression catchBody = clause.getCatchBody();
                if (catchBody == null || !isKotlinExpressionTerminal(catchBody)) {
                    return false;
                }
            }

            return true; // No bypass path exists out of this try-catch block
        }
        else if (expression instanceof KtWhenExpression) {
            KtWhenExpression whenExpr = (KtWhenExpression) expression;

            // Natively determine if an 'else' branch is present in the when expression
            boolean hasElseBranch = false;
            for (KtWhenEntry entry : whenExpr.getEntries()) {
                if (entry.isElse()) {
                    hasElseBranch = true;
                    break;
                }
            }

            // A 'when' block in Kotlin MUST have an explicit 'else' branch to be fully terminal
            if (!hasElseBranch) {
                return false;
            }

            // Loop through every single branch entry; all of them must terminate natively
            for (KtWhenEntry entry : whenExpr.getEntries()) {
                KtExpression entryExpression = entry.getExpression();
                if (entryExpression == null || !isKotlinExpressionTerminal(entryExpression)) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }


    // Helper to safely scan trailing elements in an explicit block node
    private boolean isKotlinBlockTerminal(KtBlockExpression block) {
        if (block == null) return false;
        List<KtExpression> expressions = block.getStatements();
        if (expressions.isEmpty()) return false;
        return isKotlinExpressionTerminal(expressions.get(expressions.size() - 1));
    }
    //-----------------------------------------------------------------------------


    private void generateMethodEndLogAndAdd(String methodName, String className, PsiMethod psiMethod, PsiCodeBlock methodBody) {
        //Generate the method end log statement
        String endLogStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + methodName + "," + className + "," + MethodEnd_TAG + ")\");";
        PsiStatement endLogStatementElement = factory.createStatementFromText(endLogStatement, psiMethod);

        //CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);

        //Add the method end log statement
        WriteCommandAction.runWriteCommandAction(project, () -> {
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

                // Holds the exact line number of the API call
                int lineNumber = importLogStatementAvailable ? getLineNumber(parent) + 1 : getLineNumber(parent) + 3;

                String logStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + methodCallName + "," + javaFile + "," + lineNumber + ")\");";
                PsiStatement logStatementElement = factory.createStatementFromText(logStatement, expression.getContext());

                // =========================================================================
                // 1. CONDITIONAL EVALUATION CLAUSE TRAP (e.g., if (!delete(child)) { ... })
                // =========================================================================
                PsiIfStatement parentIfCondition = PsiTreeUtil.getParentOfType(expression, PsiIfStatement.class);
                if (parentIfCondition != null && !PsiTreeUtil.isAncestor(parentIfCondition.getThenBranch(), expression, true)
                        && !PsiTreeUtil.isAncestor(parentIfCondition.getElseBranch(), expression, true)) {

                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        parentIfCondition.getParent().addBefore(logStatementElement, parentIfCondition);
                    });
                    return; // Complete handling early!
                }

                // =========================================================================
                // 2. LAMBDA EXPRESSION TRAP (e.g., pref -> editText.getText())
                // =========================================================================
                PsiLambdaExpression nestedLambda = PsiTreeUtil.getParentOfType(expression, PsiLambdaExpression.class);
                if (nestedLambda != null) {
                    PsiStatement outerStatement = PsiTreeUtil.getParentOfType(nestedLambda, PsiStatement.class);
                    if (outerStatement != null) {
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            outerStatement.getParent().addBefore(logStatementElement, outerStatement);
                        });
                        return; // Complete handling early!
                    }
                }

                // =========================================================================
                // 3. RETURN STATEMENT TRAP WITH SCOPE PROTECTION (e.g., return apiCall();)
                // =========================================================================
                PsiReturnStatement enclosingReturn = PsiTreeUtil.getParentOfType(expression, PsiReturnStatement.class);
                // SCOPE VALIDATION: Only act if the API call is actually a child of this return statement,
                // and not an independent return statement sitting higher up in a lambda block!
                if (enclosingReturn != null && PsiTreeUtil.isAncestor(enclosingReturn, parent, false)) {
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        PsiElement returnParent = enclosingReturn.getParent();

                        // Check if the return statement lives naked inside an unbraced block statement
                        if (returnParent instanceof PsiIfStatement || returnParent instanceof PsiLoopStatement) {
                            String wrappedBlockText = "{\n" + logStatement + "\n" + enclosingReturn.getText() + "\n}";
                            PsiCodeBlock newBlock = JavaPsiFacade.getElementFactory(project).createCodeBlockFromText(wrappedBlockText, enclosingReturn);
                            enclosingReturn.replace(newBlock);
                        } else {
                            // Standard block scenario (already has braces)
                            returnParent.addBefore(logStatementElement, enclosingReturn);
                        }
                    });
                    return; // Complete handling early!
                }

                // =========================================================================
                // 4. LOOK AHEAD FOR NEXT ELEMENT EXIT TRAP (e.g., apiCall(); return;)
                // =========================================================================
                PsiElement nextStatement = PsiTreeUtil.skipWhitespacesAndCommentsForward(parent);
                boolean followedByExit = false;

                // SCOPE VALIDATION: Confirm the next statement is an exit AND shares the exact same block parent container
                if (nextStatement instanceof PsiReturnStatement || nextStatement instanceof PsiThrowStatement) {
                    if (nextStatement.getParent() == parent.getParent()) {
                        followedByExit = true;
                    }
                }

                if (followedByExit) {
                    PsiElement finalNextStatement = nextStatement;
                    PsiElement finalParent = parent;
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        PsiElement grandParent = finalParent.getParent();
                        if (grandParent instanceof PsiIfStatement || grandParent instanceof PsiLoopStatement) {
                            // Convert the entire sequence layout into a clean braced block to preserve logic bounds
                            String wrappedBlockText = "{\n" + logStatement + "\n" + finalParent.getText() + "\n" + finalNextStatement.getText() + "\n}";
                            PsiCodeBlock newBlock = JavaPsiFacade.getElementFactory(project).createCodeBlockFromText(wrappedBlockText, finalParent);

                            finalNextStatement.delete();
                            finalParent.replace(newBlock);
                        } else {
                            // Safe block sequence context
                            grandParent.addBefore(logStatementElement, finalParent);
                        }
                    });
                    return; // Complete handling early!
                }

                // =========================================================================
                // 5. STANDARD INJECTION SEQUENCE PATH (Safe behind trailing semicolon)
                // =========================================================================
                PsiElement semicolon = PsiTreeUtil.nextLeaf(parent);
                while (semicolon != null && !(semicolon instanceof PsiJavaToken && ((PsiJavaToken) semicolon).getTokenType() == JavaTokenType.SEMICOLON)) {
                    semicolon = PsiTreeUtil.prevLeaf(semicolon);
                }

                if (semicolon != null) {
                    PsiElement emptyLine = parserFacade.createWhiteSpaceFromText("\n");
                    PsiElement finalInsertionPoint = semicolon.getParent();
                    PsiElement finalSemicolon = semicolon;

                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        finalInsertionPoint.addAfter(logStatementElement, finalSemicolon);
                        finalInsertionPoint.addAfter(emptyLine, finalSemicolon);
                    });
                }

            } else {
                // Log statement should be added before finish, startActivityForResult, etc.
                int lineNumber = importLogStatementAvailable ? getLineNumber(parent) + 2 : getLineNumber(parent) + 4;

                String logStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + methodCallName + "," + javaFile + "," + lineNumber + ")\");";
                PsiStatement logStatementElement = factory.createStatementFromText(logStatement, expression.getContext());

                PsiElement target = parent.getParent();
                PsiElement parentElement = parent;
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    target.addBefore(logStatementElement, parentElement);
                });
            }
        } else {
            System.out.println("[GreenMeter -> logFindViewById$ Fatal error: Method call expression is null: There is not any method call!");
        }
    }


    //This method creates and adds the log statements to the source code of the application
    private void addLogStatementToKotlinFile(KtCallExpression expression, String functionCallName, String kotlinFile) {

        System.out.println("[GreenMeter -> addLogStatementToKotlinFile$ addLogStatementToKotlinFile method is called");

        // 1. SAFE PARENT VISITOR RESOLUTION
        // Safely determine the highest relevant expression parent layout within the immediate block
        PsiElement parent = expression.getParent();
        int parentLookedUp = 0;
        PsiElement previousParent = parent;

        while (parent != null && !(parent instanceof KtBlockExpression) && !(parent instanceof KtNamedFunction)) {
            parentLookedUp++;
            previousParent = parent;
            parent = parent.getParent();
        }
        // Fall back to the immediate outer functional statement if a hard brace block wasn't present
        parent = (parent instanceof KtBlockExpression) ? previousParent : expression;

        if (parent != null && parent.isValid()) {
            int lineNumber;

            if (!"finish".equals(functionCallName) && !"startActivityForResult".equals(functionCallName)) {
                lineNumber = importLogStatementAvailable ? getLineNumber(expression) + 1 : getLineNumber(expression) + 3;

                KtPsiFactory factory = new KtPsiFactory(project);
                String logStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + functionCallName + "," + kotlinFile + "," + lineNumber + ")\")";
                KtExpression logStatementElement = factory.createExpression(logStatement);

                // =========================================================================
                // 1. CONDITIONAL EVALUATION CLAUSE TRAP (e.g., if (!delete(child)) { ... })
                // =========================================================================
                KtIfExpression parentKotlinIf = PsiTreeUtil.getParentOfType(expression, KtIfExpression.class);
                if (parentKotlinIf != null && parentKotlinIf.getCondition() != null
                        && PsiTreeUtil.isAncestor(parentKotlinIf.getCondition(), expression, false)) {

                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        parentKotlinIf.getParent().addBefore(logStatementElement, parentKotlinIf);
                        parentKotlinIf.getParent().addBefore(factoryKotlin.createNewLine(), parentKotlinIf);
                    });
                    return; // Complete handling early!
                }

                // =========================================================================
                // 2. LAMBDA EXPRESSION TRAP (e.g., pref -> editText.getText())
                // =========================================================================
                KtLambdaExpression nestedKotlinLambda = PsiTreeUtil.getParentOfType(expression, KtLambdaExpression.class);
                if (nestedKotlinLambda != null) {
                    KtExpression outerKotlinExpression = PsiTreeUtil.getParentOfType(nestedKotlinLambda, KtExpression.class);
                    if (outerKotlinExpression != null) {
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            outerKotlinExpression.getParent().addBefore(logStatementElement, outerKotlinExpression);
                            outerKotlinExpression.getParent().addBefore(factoryKotlin.createNewLine(), outerKotlinExpression);
                        });
                        return; // Complete handling early!
                    }
                }

                // =========================================================================
                // 3. RETURN EXPRESSION TRAP WITH SCOPE PROTECTION (e.g., return apiCall())
                // =========================================================================
                KtReturnExpression enclosingKotlinReturn = PsiTreeUtil.getParentOfType(expression, KtReturnExpression.class);
                // SCOPE VALIDATION: Verify the API call is an actual child of this return expression
                if (enclosingKotlinReturn != null && PsiTreeUtil.isAncestor(enclosingKotlinReturn, parent, false)) {
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        PsiElement returnParent = enclosingKotlinReturn.getParent();

                        // Kotlin tracks braceless scopes within KtContainerNode or directly under parent If/Loop structures
                        if (returnParent instanceof KtContainerNode || returnParent instanceof KtIfExpression || returnParent instanceof KtLoopExpression) {
                            String wrappedKotlinBlock = "{\n" + logStatement + "\n" + enclosingKotlinReturn.getText() + "\n}";
                            KtExpression newKotlinBlock = factory.createBlock(wrappedKotlinBlock);
                            enclosingKotlinReturn.replace(newKotlinBlock);
                        } else {
                            // Safely wrapped inside braces already
                            returnParent.addBefore(logStatementElement, enclosingKotlinReturn);
                            returnParent.addBefore(factory.createNewLine(), enclosingKotlinReturn);
                        }
                    });
                    return; // Complete handling early!
                }

                // =========================================================================
                // 4. LOOK AHEAD FOR NEXT ELEMENT EXIT TRAP (e.g., apiCall(); return)
                // =========================================================================
                PsiElement nextExpression = PsiTreeUtil.skipWhitespacesAndCommentsForward(parent);
                boolean followedByExit = false;

                // SCOPE VALIDATION: Confirm next element is an exit expression AND belongs to the same parent container
                if (nextExpression instanceof KtReturnExpression || nextExpression instanceof KtThrowExpression) {
                    if (nextExpression.getParent() == parent.getParent()) {
                        followedByExit = true;
                    }
                }

                if (followedByExit) {
                    PsiElement finalNextExpression = nextExpression;
                    PsiElement finalParent2 = parent;
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        PsiElement grandParent = finalParent2.getParent();
                        if (grandParent instanceof KtContainerNode || grandParent instanceof KtIfExpression || grandParent instanceof KtLoopExpression) {
                            // Structure into a combined code block to protect conditional scope matching
                            String wrappedKotlinBlock = "{\n" + logStatement + "\n" + finalParent2.getText() + "\n" + finalNextExpression.getText() + "\n}";
                            KtExpression newKotlinBlock = factory.createBlock(wrappedKotlinBlock);

                            finalNextExpression.delete();
                            finalParent2.replace(newKotlinBlock);
                        } else {
                            // Standard safe sequence block block injection point
                            grandParent.addBefore(logStatementElement, finalParent2);
                            grandParent.addBefore(factory.createNewLine(), finalParent2);
                        }
                    });
                    return; // Complete handling early!
                }

                // =========================================================================
                // 5. STANDARD INJECTION SEQUENCE PATH (Safe downstream insertion)
                // =========================================================================
                PsiElement finalParent = parent;
                int finalParentLookedUp = parentLookedUp;

                WriteCommandAction.runWriteCommandAction(project, () -> {
                    if (finalParentLookedUp > 1) {
                        finalParent.addAfter(factory.createNewLine(), expression);
                    }

                    PsiElement addedElement = finalParent.addAfter(logStatementElement, expression);
                    finalParent.addAfter(factory.createNewLine(), expression);

                    // Reformat the added element layout cleanly using current project styles
                    CodeStyleManager.getInstance(project).reformat(addedElement);
                });

            } else {
                // Handle special case execution layouts (finish, startActivityForResult)
                lineNumber = importLogStatementAvailable ? getLineNumber(expression) + 2 : getLineNumber(expression) + 4;

                KtPsiFactory factory = new KtPsiFactory(project);
                String logStatement = "Log.d(\"" + Logging_TAG + "\", \"(" + functionCallName + "," + kotlinFile + "," + lineNumber + ")\")";
                KtExpression logStatementElement = factory.createExpression(logStatement);

                PsiElement finalParent1 = parent;
                KtCallExpression finalExpression1 = expression;
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    finalParent1.addBefore(logStatementElement, finalExpression1);
                    finalParent1.addBefore(factory.createNewLine(), finalExpression1);
                });
            }

        } else {
            System.out.println("[GreenMeter -> addLogStatementToKotlinFile$ Fatal Error: Function call expression is null: There is no function call!");
        }
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
