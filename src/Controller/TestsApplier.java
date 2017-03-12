package Controller;

import Model.Task;
import Model.Tests;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by GSatosov on 3/4/2017.
 */
class TestsApplier {
    private static String applyTests(Tests tests) throws IOException, InterruptedException {
        ArrayList<String> output = new ArrayList<>();
        Task task = tests.getTask();
        HashMap<String, ArrayList<String>> testContents = tests.getTestContents();
        Process p = Runtime.getRuntime().exec("ghci " + task.getSourcePath());
        PrintStream cmdInput = new PrintStream(p.getOutputStream());
        InputStream cmdOutput = p.getInputStream();
        InputStream cmdErrors = p.getErrorStream();
        new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(cmdOutput))) {
                String ch;
                while (true) {
                    ch = r.readLine();
                    if (ch == null) continue;
                    output.add(ch);
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }).start();
        new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(cmdErrors))) {
                String ch;
                while (true) {
                    ch = r.readLine();
                    output.add(ch);
                    if (ch == null) break;
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }).start();
        int max = testContents.size();
        for (int i = 0; i < 6; i++) {
            if (i == 5) return "TL";
            if (output.stream().anyMatch(a -> a.startsWith("Ok, modules loaded:")))
                break;
            Thread.sleep(500);
        }
        long testingScore = testContents.entrySet().stream().filter(a -> {
            String k = a.getKey();
            ArrayList<String> values = a.getValue();
            cmdInput.println(task.getName() + " " + k);
            cmdInput.flush();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String response = output.get(output.size() - 1).split(" ", 2)[1]; // *>TaskName> Output
            return values.contains(response);
        }).count();
        return testingScore + "/" + max;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Task task1 = new Task("hasPair", "FP", "e:/study/haskell/hometask1/HasPair.hs"); //Testing dummy
        HashMap<String, ArrayList<String>> contents = new HashMap<>();
        ArrayList<String> truth = new ArrayList<>();
        ArrayList<String> notTruth = new ArrayList<>();
        truth.add("True");
        notTruth.add("False");
        contents.put("1100", truth);
        contents.put("1212", notTruth);
        System.out.print(applyTests(new Tests(task1, contents)));
    }
}