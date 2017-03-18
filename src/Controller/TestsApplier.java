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

    private String failResponse(String response, String sourcePath) {
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
            int compilationTime = 0;
            while (true) {
                compilationTime++;
                if (!output.isEmpty() && output.get(output.size() - 1).startsWith("Ok, modules loaded:"))
                    break;
                if (!output.isEmpty() && output.get(output.size() - 1).startsWith("Failed, modules loaded: none."))
                    return failResponse("CE", task.getSourcePath()); //Compilation Error
                if (compilationTime == 200) {
                    return failResponse("TL",task.getSourcePath());
                }
                try {
                    Thread.sleep(10);
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
            String result = testingScore + "/" + maxScore;
            if (testingScore < maxScore)
                return failResponse(result, task.getSourcePath());
            return result;
        }).collect(Collectors.toList());
        cmdInput.close();
        notInterrupted = false;
        cmdOutputThread.interrupt();
        return results;
    }
}