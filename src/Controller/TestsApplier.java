package Controller;

import Model.Result;
import Model.Task;
import Model.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for applying tests. As of 3/14 only Haskell support is implemented.
 */
class TestsApplier {
    private volatile boolean notInterrupted;
    private ArrayList<String> haskellOutput = new ArrayList<>();

    private Thread cmdOutput(InputStream stream) {
        return new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(stream))) {
                String ch;
                while (notInterrupted) {
                    ch = r.readLine();
                    if (ch == null) continue;
                    haskellOutput.add(ch);
                    System.out.println(ch);
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    private Result failResult(String response, Task task) {
        File f = new File(task.getSourcePath());
        if (f.delete())
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
            if (!haskellOutput.isEmpty())
                break;
            Thread.sleep(10);
        }
        if (haskellOutput.get(0).startsWith("'ghci' is not recognized as an internal or external command")) {
            System.out.print("Add ghci to your PATH before proceeding.");
            return new ArrayList<>(); //TODO Think of a better way.
        }
        List<Result> results = tasks.stream().map(task -> {
            haskellOutput.clear();
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
                if (!haskellOutput.isEmpty() && haskellOutput.get(haskellOutput.size() - 1).startsWith("Ok, modules loaded:"))
                    break;
                if (!haskellOutput.isEmpty() && haskellOutput.get(haskellOutput.size() - 1).startsWith("Failed, modules loaded: none."))
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
                int beforeTesting = haskellOutput.size();
                String testCommand = String.valueOf(functionToTest) + " " + test.getInput();
                ArrayList<String> testOutputVariants = test.getOutputVariants();
                System.out.println(testCommand);
                cmdInput.println(testCommand);
                cmdInput.flush();
                int computationTime = 0;
                while (true) {
                    computationTime++;
                    if (haskellOutput.size() > beforeTesting) break;
                    if (computationTime == 150) return false; //Took too long to compute.
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                String response = haskellOutput.get(beforeTesting).split(" ", 2)[1]; // *>TaskName> Output
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
        haskellOutput.clear();
        p.destroy();
        return results;
    }

    private Result applyJavaTests(Task task) throws IOException, URISyntaxException, ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, InterruptedException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, System.out, System.out, task.getSourcePath());
        List<String> compileCommands = new ArrayList<>();
        String parentFolder = new File(task.getSourcePath()).getParent();
        compileCommands.add("java");
        compileCommands.add("-cp");
        compileCommands.add(parentFolder);
        compileCommands.add(task.getName());
        ProcessBuilder pb = new ProcessBuilder(compileCommands);
        pb.redirectErrorStream(true);
        File inputFile = new File(parentFolder + "\\input.txt");
        if (inputFile.createNewFile())
            System.out.println("File at " + inputFile.getAbsolutePath() + " was successfully created.");
        File outputFile = new File(parentFolder + "\\output.txt");
        if (outputFile.createNewFile())
            System.out.println("File at " + outputFile.getAbsolutePath() + " was successfully created.");
        pb.redirectInput(inputFile);
        pb.redirectOutput(outputFile);
        FileWriter writer = new FileWriter(inputFile);
        BufferedReader reader = new BufferedReader(new FileReader(outputFile));
        ArrayList<Test> testContents = task.getTestContents();
        int maxScore = testContents.size();
        long curScore = testContents.stream().filter(test -> {
            try {
                int computationTime = 0;
                writer.write(test.getInput());
                writer.flush();
                pb.start();
                String testOutput;
                while (true) {
                    testOutput = reader.readLine();
                    if (testOutput != null)
                        break;
                    if (computationTime == 200)
                        return false;
                    Thread.sleep(10);
                    computationTime++;
                }
                return test.getOutputVariants().contains(testOutput);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }).count();
        if (inputFile.delete())
            System.out.println("File at " + inputFile.getAbsolutePath() + " was successfully deleted.");
        if (outputFile.delete())
            System.out.println("File at " + outputFile.getAbsolutePath() + " was successfully deleted.");
        return new Result(curScore + "/" + maxScore, task);
    }

    public static void main(String[] args) throws ClassNotFoundException, URISyntaxException, IOException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InterruptedException {
        Task task = new Task("testingDummy", "123", "data\\Функциональное_программирование\\A3401\\Назаров_Арсений\\testingDummy.java");
        ArrayList<Test> tests = new ArrayList<>();
        ArrayList<String> outputVariants = new ArrayList<>();
        outputVariants.add("6");
        tests.add(new Test("3 3", outputVariants));
        task.setTestContents(tests);
        System.out.println(new TestsApplier().applyJavaTests(task));
    }
}
