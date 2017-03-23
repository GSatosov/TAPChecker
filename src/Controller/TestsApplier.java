package Controller;

import Model.Result;
import Model.Task;
import Model.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for applying tests. As of 3/14 only Haskell support is implemented.
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
        File f = new File(task.getSourcePath());
        if (f.delete())
            System.out.println("File at " + task.getSourcePath() + " has been successfully deleted.");
        return new Result(response, task);
    }

    List<Result> applyTests(ArrayList<Task> tasks) throws IOException, InterruptedException {
        notInterrupted = true;
        ProcessBuilder builder = new ProcessBuilder("ghci");
        builder.redirectErrorStream(true);
        Process p = builder.start();
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
                String testInput = test.getInput();
                ArrayList<String> testOutputVariants = test.getOutputVariants();
                System.out.println(new String(functionToTest) + " " + testInput);
                cmdInput.println(new String(functionToTest) + " " + testInput);
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
            return new Result(taskResult,task);
        }).collect(Collectors.toList());
        cmdInput.close();
        notInterrupted = false;
        cmdOutputThread.interrupt();
        output.clear();
        return results;
    }
}
