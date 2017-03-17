package Controller;

import Model.Task;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
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

    private String failedTests(String response, String sourcePath) {
        File f = new File(sourcePath);
        if (f.delete())
            System.out.println("File at " + sourcePath + " has been successfully deleted.");
        return response;
    }

    List<String> applyTests(ArrayList<Task> tasks) throws IOException, InterruptedException {
        notInterrupted = true;
        ProcessBuilder builder = new ProcessBuilder("ghci");
        builder.redirectErrorStream(true);
        Process p = builder.start();
        PrintStream cmdInput = new PrintStream(p.getOutputStream());
        InputStream cmdOutputStream = p.getInputStream();
        Thread cmdOutputThread = cmdOutput(cmdOutputStream);
        cmdOutputThread.start();
        List<String> results = tasks.stream().map(task -> {
            HashMap<String, ArrayList<String>> testContents = task.getTestContents();
            char[] functionToTest = task.getName().split("\\.")[0].toCharArray(); //TaskName.hs -> taskName
            functionToTest[0] = Character.toLowerCase(functionToTest[0]);
            cmdInput.println(":l " + task.getSourcePath());
            cmdInput.flush();
            int maxScore = testContents.size();
            for (int i = 0; i < 6; i++) {
                if (i == 5) return failedTests("TL", task.getSourcePath()); //Took too long to compile
                if (!output.isEmpty() && output.get(output.size() - 1).startsWith("Ok, modules loaded:"))
                    break;
                if (!output.isEmpty() && output.get(output.size() - 1).startsWith("Failed, modules loaded: none."))
                    return failedTests("CE", task.getSourcePath()); //Compilation Error

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            long testingScore = testContents.entrySet().stream().filter(a -> {
                int beforeTesting = output.size();
                String testInput = a.getKey();
                ArrayList<String> testOutputVariants = a.getValue();
                System.out.println(new String(functionToTest) + " " + testInput);
                cmdInput.println(new String(functionToTest) + " " + testInput);
                cmdInput.flush();
                int time = 0;
                while (true) {
                    time++;
                    if (output.size() > beforeTesting) break;
                    if (time == 200) return false; //Took too long to compute.
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
            String result = testingScore + "/" + maxScore;
            if (testingScore < maxScore)
                return failedTests(result, task.getSourcePath());
            return result;
        }).collect(Collectors.toList());
        cmdInput.close();
        notInterrupted = false;
        cmdOutputThread.interrupt();
        return results;
    }
}