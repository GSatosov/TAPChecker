package Controller;

import Model.Result;
import Model.Task;
import Model.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for applying tests. As of 3/14 only Haskell support is implemented. As of 4/18 Java support is implemented as well. TODO handle exceptions better
 */
class TestsApplier {
    private volatile boolean notInterrupted;
    private ArrayList<String> output = new ArrayList<>();
    private Process haskellProcess;
    private PrintStream cmdInput;

    private void clearFolder(Task task, File inputFile, File outputFile, File errorFile) {
        if (inputFile.delete())
            System.out.println("File at " + inputFile.getAbsolutePath() + " was successfully deleted.");
        if (outputFile.delete())
            System.out.println("File at " + outputFile.getAbsolutePath() + " was successfully deleted.");
        if (errorFile.delete())
            System.out.println("Error file for " + task.getSourcePath() + " has been successfully deleted.");
        if (new File(task.getSourcePath().substring(0, task.getSourcePath().length() - 4) + "class").delete())
            System.out.println("Class file for " + task.getSourcePath() + " has been successfully deleted.");
    }

    private Thread cmdOutput(InputStream stream) {
        return new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(stream))) {
                String ch;
                while (notInterrupted) {
                    ch = r.readLine();
                    if (ch == null) continue;
                    output.add(ch);
                    System.out.println(ch);
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    private void startHaskellProcess() {
        notInterrupted = true;
        try {
            haskellProcess = new ProcessBuilder("ghci").redirectErrorStream(true).start();
            cmdInput = new PrintStream(haskellProcess.getOutputStream());
            cmdOutput(haskellProcess.getInputStream()).start();
            while (true) { //On first launch ProcessBuilder takes a lot of time to execute first command.
                if (!output.isEmpty())
                    break;
                Thread.sleep(10);
            }
            if (output.get(0).startsWith("'ghci' is not recognized as an internal or external command"))
                throw new IOException();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Add ghci to your path before proceeding.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Result handleHaskellTask(Task task) {
        output.clear();
        ArrayList<Test> testContents = task.getTestContents();
        char[] functionToTest = task.getName().split("\\.")[0].toCharArray(); //TaskName.hs -> taskName
        functionToTest[0] = Character.toLowerCase(functionToTest[0]);
        cmdInput.println(":l " + task.getSourcePath());
        System.out.println(":l " + task.getSourcePath());
        cmdInput.flush();
        int compilationTime = 0;
        while (true) {
            compilationTime++;
            if (!output.isEmpty() && output.get(output.size() - 1).startsWith("Ok, modules loaded:"))
                break;
            if (!output.isEmpty() && output.get(output.size() - 1).startsWith("Failed, modules loaded: none."))
                return new Result("CE", task); //Compilation Error
            if (compilationTime == 100) {
                notInterrupted = false;
                haskellProcess.destroy();
                startHaskellProcess();
                return new Result("TL", task); // Took too long to compile.
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int testingScore = 0;
        int maxScore = testContents.size();
        for (Test test : testContents) {
            int beforeTesting = output.size();
            String testCommand;
            if (test.getInput().contains(";")) {
                String[] testParts = test.getInput().split(";");
                testCommand = testParts[0] + " (" + String.valueOf(functionToTest) + testParts[1] + " )";
            } else
                testCommand = String.valueOf(functionToTest) + " " + test.getInput();
            ArrayList<String> testOutputVariants = test.getOutputVariants();
            System.out.println(testCommand);
            cmdInput.println(testCommand);
            cmdInput.flush();
            int computationTime = 0;
            while (true) {
                computationTime++;
                if (output.size() > beforeTesting) break;
                if (computationTime == test.getTime() * 100) {
                    notInterrupted = false;
                    haskellProcess.destroy();
                    startHaskellProcess();
                    return new Result("TL", task); //Took too long to compute.
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            String response = output.get(beforeTesting).split(" ", 2)[1]; // *>TaskName> Output
            if (testOutputVariants.contains("Error") && response.startsWith("*** Exception") // If exception is expected.
                    || testOutputVariants.contains(response))
                testingScore++;
        }
        String taskResult = testingScore + "/" + maxScore;
        return new Result(taskResult, task);
    }

    List<Result> applyHaskellTests(ArrayList<Task> tasks) throws IOException, InterruptedException {
        startHaskellProcess();
        List<Result> results =  tasks.stream().map(this::handleHaskellTask).collect(Collectors.toList());
        notInterrupted = false;
        haskellProcess.destroy();
        cmdInput.close();
        return results;
    }

    private Result handleJavaTask(Task task, JavaCompiler compiler) throws IOException {
        char[] functionToTest = task.getName().split("\\.")[0].toCharArray(); //TaskName.hs -> taskName
        functionToTest[0] = Character.toLowerCase(functionToTest[0]);
        String parentFolder = new File(task.getSourcePath()).getParent();
        File errorFile = new File(parentFolder + "\\error.txt");
        FileOutputStream errorStream = new FileOutputStream(errorFile);
        compiler.run(null, System.out, errorStream, task.getSourcePath());
        errorStream.close();
        BufferedReader br = new BufferedReader(new FileReader(errorFile));
        if (br.readLine() != null) {
            br.close();
            if (errorFile.delete())
                System.out.println("Error file for " + task.getSourcePath() + " has been successfully deleted.");
            return new Result("CE", task);
        }
        List<String> compilationCommands = new ArrayList<>();
        compilationCommands.add("java");
        compilationCommands.add("-cp");
        compilationCommands.add(parentFolder);
        compilationCommands.add(String.valueOf(functionToTest));
        ProcessBuilder pb = new ProcessBuilder(compilationCommands);
        pb.redirectError(errorFile);
        File inputFile = new File(parentFolder + "\\input.txt");
        File outputFile = new File(parentFolder + "\\output.txt");
        try {
            if (inputFile.createNewFile())
                System.out.println("File at " + inputFile.getAbsolutePath() + " was successfully created.");
            if (outputFile.createNewFile())
                System.out.println("File at " + outputFile.getAbsolutePath() + " was successfully created.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        pb.redirectInput(inputFile);
        pb.redirectOutput(outputFile);
        ArrayList<Test> testContents = task.getTestContents();
        int maxScore = testContents.size();
        int curScore = 0;
        for (Test test : testContents) {
            try {
                int computationTime = 0;
                FileWriter writer = new FileWriter(inputFile); //To clear input.
                FileWriter outputCleaner = new FileWriter(outputFile);
                outputCleaner.close(); //To clear output
                BufferedReader reader = new BufferedReader(new FileReader(outputFile));
                writer.write(test.getInput());
                writer.close();
                pb.start();
                String testOutput;
                boolean tlCheck;
                while (true) {
                    testOutput = reader.readLine();
                    if (testOutput != null)
                        break;
                    tlCheck = computationTime == test.getTime() * 100;
                    if (errorFile.length() != 0 || tlCheck) {
                        FileWriter errorCleaner = new FileWriter(errorFile);
                        errorCleaner.close(); //Clean errorFile.
                        reader.close(); //Close inputFile
                        clearFolder(task, inputFile, outputFile, errorFile);
                        if (tlCheck)
                            return new Result("TL", task); //Time Limit.
                        return new Result("IH", task); //Input Handling Error
                    }
                    Thread.sleep(10);
                    computationTime++;
                }
                reader.close();
                if (test.getOutputVariants().contains(testOutput))
                    curScore++;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        clearFolder(task, inputFile, outputFile, errorFile);
        String taskResult = curScore + "/" + maxScore;
        if (curScore < maxScore)
            return new Result(taskResult, task);
        return new Result(curScore + "/" + maxScore, task);
    }

    List<Result> applyJavaTests(ArrayList<Task> tasks) throws IOException, InterruptedException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        return tasks.stream().map(task -> {
            try {
                return handleJavaTask(task, compiler);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new Result("asd", task);
        }).collect(Collectors.toList());
    }
}
