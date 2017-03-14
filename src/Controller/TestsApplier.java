package Controller;

import Model.Task;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class for applying tests. As of 3/14 only Haskell support is implemented.
 */
class TestsApplier {
    private volatile boolean notInterrupted;

     String applyTests(Task task) throws IOException, InterruptedException {
        ArrayList<String> output = new ArrayList<>();
        HashMap<String, ArrayList<String>> testContents = task.getTestContents();
        char[] functionToTest = task.getName().toCharArray();
        functionToTest[0] = Character.toLowerCase(functionToTest[0]);
        ProcessBuilder builder = new ProcessBuilder("ghci");
        builder.redirectErrorStream(true);
        Process p = builder.start();
        PrintStream cmdInput = new PrintStream(p.getOutputStream());
        InputStream cmdOutput = p.getInputStream();
        cmdInput.println(":l " + task.getSourcePath());
        cmdInput.flush();
        notInterrupted = true;
        Thread thread = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(cmdOutput))) {
                String ch;
                while (notInterrupted) {
                    ch = r.readLine();
                    if (ch == null) continue;
                    output.add(ch);
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });
        thread.start();
        int maxScore = testContents.size();
        for (int i = 0; i < 6; i++) {
            if (i == 5) return "TL"; //Time Limit
            if (output.stream().anyMatch(a -> a.startsWith("Ok, modules loaded:")))
                break;
            Thread.sleep(500);
        }
        long testingScore = testContents.entrySet().stream().filter(a -> {
            int beforeCommand = output.size();
            String testInput = a.getKey();
            ArrayList<String> taskOutputs = a.getValue();
            cmdInput.println(new String(functionToTest) + " " + testInput);
            cmdInput.flush();
            int time = 0;
            while (true) {
                time++;
                if (output.size() > beforeCommand) break;
                if (time == 200) return false; //Took too long to compute.
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            String response = output.get(output.size() - 1).split(" ", 2)[1]; // *>TaskName> Output
            return taskOutputs.contains(response);
        }).count();
        notInterrupted = false;
        thread.interrupt();
        //       output.forEach(System.out::println);
        return testingScore + "/" + maxScore;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Task task1 = new Task("HasPair", "FP", "e:/study/haskell/hometask1/HasPair.hs"); //Testing dummy
        HashMap<String, ArrayList<String>> contents = new HashMap<>();
        ArrayList<String> truth = new ArrayList<>();
        ArrayList<String> notTruth = new ArrayList<>();
        truth.add("True");
        notTruth.add("False");
        contents.put("1100", truth);
        contents.put("1212", notTruth);
        task1.setTestContents(contents);
        System.out.print(new TestsApplier().applyTests(task1));
    }
}