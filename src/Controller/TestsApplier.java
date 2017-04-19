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
 * Class for applying tests. As of 3/14 only Haskell support is implemented. As of 4/18 Java support is implemented as well.
 */
class TestsApplier {
    private volatile boolean notInterrupted;
    private ArrayList<String> output = new ArrayList<>();

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

    private Result failResult(String response, Task task) {
        if (new File(task.getSourcePath()).delete())
            System.out.println("File at " + task.getSourcePath() + " has been successfully deleted.");
        return new Result(response, task);
    }

    List<Result> applyHaskellTests(ArrayList<Task> tasks) throws IOException, InterruptedException {
        notInterrupted = true;
        Process p = new ProcessBuilder("ghci").redirectErrorStream(true).start();
        PrintStream cmdInput = new PrintStream(p.getOutputStream());
        InputStream cmdOutputStream = p.getInputStream();
        Thread cmdOutputThread = cmdOutput(cmdOutputStream);
        cmdOutputThread.start();
        while (true) { //On first launch ProcessBuilder takes a lot of time to execute first command.
            if (!output.isEmpty())
                break;
            Thread.sleep(10);
        }
        if (output.get(0).startsWith("'ghci' is not recognized as an internal or external command")) {
            System.out.print("Add ghci to your PATH before proceeding.");
            return new ArrayList<>(); //TODO Think of a better way.
        }
        List<Result> results = tasks.stream().map(task -> {
            output.clear();
            ArrayList<Test> testContents = task.getTestContents();
            char[] functionToTest = task.getName().split("\\.")[0].toCharArray(); //TaskName.hs -> taskName
            functionToTest[0] = Character.toLowerCase(functionToTest[0]);
            cmdInput.println(":l " + task.getSourcePath());
            System.out.println(":l " + task.getSourcePath());
            cmdInput.flush();
            int maxScore = testContents.size();
            int compilationTime = 0;
            while (true) {
                compilationTime++;
                if (!output.isEmpty() && output.get(output.size() - 1).startsWith("Ok, modules loaded:"))
                    break;
                if (!output.isEmpty() && output.get(output.size() - 1).startsWith("Failed, modules loaded: none."))
                    return failResult("CE", task); //Compilation Error
                if (compilationTime == 200) {
                    return failResult("TL", task); // Took too long to compile.
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            long testingScore = testContents.stream().filter(test -> {
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
                    if (computationTime == 150) return false; //Took too long to compute.
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                String response = output.get(beforeTesting).split(" ", 2)[1]; // *>TaskName> Output
                return testOutputVariants.contains("Error") && response.startsWith("*** Exception") // If exception is expected.
                        || testOutputVariants.contains(response);
            }).count();
            String taskResult = testingScore + "/" + maxScore;
            if (testingScore < maxScore)
                return failResult(taskResult, task);
            return new Result(taskResult, task);
        }).collect(Collectors.toList());
        cmdInput.close();
        notInterrupted = false;
        cmdOutputThread.interrupt();
        output.clear();
        return results;
    }

    List<Result> applyJavaTests(ArrayList<Task> tasks) throws IOException, InterruptedException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<Result> results = new ArrayList<>();
        for (Task task : tasks) {
            char[] functionToTest = task.getName().split("\\.")[0].toCharArray(); //TaskName.hs -> taskName
            functionToTest[0] = Character.toLowerCase(functionToTest[0]);
            String parentFolder = new File(task.getSourcePath()).getParent();
            File errorFile = new File(parentFolder + "\\error.txt");
            FileOutputStream errorStream = new FileOutputStream(errorFile);
            compiler.run(null, System.out, errorStream, task.getSourcePath());
            errorStream.close();
            BufferedReader br = new BufferedReader(new FileReader(errorFile));
            if (br.readLine() != null) {
                results.add(failResult("CE", task));
                br.close();
                if (errorFile.delete())
                    System.out.println("Error file for " + task.getSourcePath() + " has been successfully deleted.");
                continue;
            }
            br.close();
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
            boolean encounteredIH = false;
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
                    while (true) {
                        testOutput = reader.readLine();
                        if (testOutput != null)
                            break;
                        if (computationTime == 200)
                            continue;
                        if (errorFile.length() != 0) {
                            FileWriter errorCleaner = new FileWriter(errorFile);
                            errorCleaner.close(); //Clean errorFile.
                            results.add(failResult("IH", task)); //Input Handling Error
                            encounteredIH = true;
                            break;
                        }
                        Thread.sleep(10);
                        computationTime++;
                    }
                    reader.close();
                    if (encounteredIH)
                        break; //Encountered IH error earlier.
                    if (test.getOutputVariants().contains(testOutput))
                        curScore++;
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (inputFile.delete())
                System.out.println("File at " + inputFile.getAbsolutePath() + " was successfully deleted.");
            if (outputFile.delete())
                System.out.println("File at " + outputFile.getAbsolutePath() + " was successfully deleted.");
            if (errorFile.delete())
                System.out.println("Error file for " + task.getSourcePath() + " has been successfully deleted.");
            if (new File(task.getSourcePath().substring(0, task.getSourcePath().length() - 4) + "class").delete())
                System.out.println("Class file for " + task.getSourcePath() + " has been successfully deleted.");
            if (encounteredIH)
                continue; //Encountered IH error earlier.
            String taskResult = curScore + "/" + maxScore;
            if (curScore < maxScore)
                results.add(failResult(taskResult, task));
            else
                results.add(new Result(curScore + "/" + maxScore, task));
        }
        return results;
    }
}
