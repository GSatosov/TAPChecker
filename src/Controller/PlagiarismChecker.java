package Controller;

import Model.PlagiarismResult;
import Model.Task;
import View.MainController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Class for checking tasks on plagiarism using Levenshtein distance algorithm.
 */
class PlagiarismChecker {
    private List<ArrayList<Task>> taskList;
    private ArrayList<PlagiarismResult> plagiarismResults;

    PlagiarismChecker(List<ArrayList<Task>> tasks, ArrayList<PlagiarismResult> results) {
        this.taskList = tasks;
        this.plagiarismResults = results;
    }

    ArrayList<PlagiarismResult> start() {
        ArrayList<PlagiarismResult> results = new ArrayList<>();
        MainController.println("Starting plagiarism check.");
        CountDownLatch latch = new CountDownLatch(taskList.size());
        taskList.forEach(list -> new Thread(() -> {
            results.addAll(handleListOfTasks(list));
            latch.countDown();
        }).start());
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return results;
    }

    private ArrayList<PlagiarismResult> handleListOfTasks(ArrayList<Task> tasks) {
        ArrayList<PlagiarismResult> results = new ArrayList<>();
        for (int i = 0; i < tasks.size() - 1; i++) {
            Task taskToCheck = tasks.get(i);
            for (int j = i + 1; j < tasks.size(); j++) {
                if (!plagiarismResults.contains(new PlagiarismResult(taskToCheck, tasks.get(j))))
                    try {
                        results.add(calculatePlagiarismProbability(taskToCheck, tasks.get(j)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
        MainController.println(tasks.get(0).getName() + " has been checked for plagiarism");
        return results;
    }

    private PlagiarismResult calculatePlagiarismProbability(Task task1, Task task2) throws IOException {
        String preparedFirstTask = prepareTask(task1);
        String preparedSecondTask = prepareTask(task2);
        int maxDistance = Math.max(preparedFirstTask.length(), preparedSecondTask.length());
        int distance = LevenshteinDistance(preparedFirstTask, preparedSecondTask);
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        double result = (1 - (distance * 1.0 / maxDistance)) * 100;
        return new PlagiarismResult(nf.format(result) + "%", task1, task2);
    }

    //Removes comments (currently one-line comments) and lines with module name/class name/public static void main.
    private String prepareTask(Task task) throws IOException {
        ArrayList<String> lines = new ArrayList<>(Files.readAllLines(Paths.get(task.getSourcePath())));
        if (task.getName().endsWith("hs")) {
            char[] taskName = task.getName().split("\\.")[0].toCharArray();
            taskName[0] = Character.toLowerCase(taskName[0]);
            return removeMultiLineComments(lines.stream().filter(line -> !line.isEmpty()).map(line -> {
                if (line.trim().contains("--"))
                    return line.substring(0, line.indexOf("--"));
                else return line;
            }).filter(line -> !line.trim().startsWith("module") && !line.trim().contains(new String(taskName) + " ::"))
                    .reduce("", String::concat), "{-", "-}").replaceAll("\\s+", " ");
        }
        if (lines.stream().filter(line -> line.contains("{")).count() >= 2) {
            ArrayList<String> finalLines = lines;
            lines = lines.stream().map(line -> {
                if (line.equals(finalLines.stream().filter(line1 -> line1.contains("{")).skip(1).findFirst().get()))
                    return "}".concat(line);
                else
                    return line;
            }).collect(Collectors.toCollection(ArrayList::new));
        }
        String taskWithoutComments = removeMultiLineComments(lines.stream().map(line -> {
            if (line.trim().contains("//"))
                return line.substring(0, line.indexOf("//"));
            else return line;
        }).reduce("", String::concat), "/*", "*/")
                .replaceAll("\\s+", " ");
        int openingFigureBracketFirstOccurrence = taskWithoutComments.indexOf("{");
        int closingFigureBracketFirstOccurrence = taskWithoutComments.indexOf("}");
        String firstSection = taskWithoutComments.substring(0, openingFigureBracketFirstOccurrence);
        String secondSection = taskWithoutComments.substring(openingFigureBracketFirstOccurrence + 1, closingFigureBracketFirstOccurrence);
        String concatenatedSections = firstSection
                .concat(secondSection)
                .concat(parseJavaFileIntoSections(new ArrayList<>(), taskWithoutComments.substring(taskWithoutComments.indexOf("{", openingFigureBracketFirstOccurrence + 1), taskWithoutComments.length())));
        return concatenatedSections.replace("public static void main(String[] args)", "")
                .replace("public static void main (String[] args)", "")
                .replace("public class " + task.getName().split("\\.")[0], "")
                .replace("class " + task.getName().split("\\.")[0], "");
    }

    private String removeMultiLineComments(String concatenatedLines, String delimiter1, String delimiter2) { //Example : {- ... -} for Haskell comments, /* ... */ for Java comments.
        if ((concatenatedLines.contains(delimiter1) && concatenatedLines.contains(delimiter2))) {
            int leftIndex = concatenatedLines.indexOf(delimiter1);
            int rightIndex = concatenatedLines.indexOf(delimiter2);
            return (removeMultiLineComments(concatenatedLines.substring(0, leftIndex)
                    .concat(concatenatedLines.substring(rightIndex + delimiter2.length(), concatenatedLines.length())), delimiter1, delimiter2));
        }
        return concatenatedLines;
    }

    private String parseJavaFileIntoSections(ArrayList<String> sections, String remainder) {
        int leftIndex = remainder.indexOf("{");
        int balance = 1;
        int rightIndex = leftIndex + 1;
        while (rightIndex < remainder.length()) {
            if (remainder.charAt(rightIndex) == '{')
                balance++;
            if (remainder.charAt(rightIndex) == '}')
                balance--;
            if (balance == 0)
                break;
            rightIndex++;
        }
        if (rightIndex == remainder.length()) { //For extreme cases
            sections.sort((o1, o2) -> o2.length() - o1.length());
            sections.add(remainder);
            return sections.stream().reduce("", String::concat);
        }
        sections.add(remainder.substring(leftIndex + 1, rightIndex));
        String newRemainder = remainder.substring(rightIndex + 1, remainder.length());
        if (!newRemainder.contains("{")) {
            sections.sort((o1, o2) -> o2.length() - o1.length());
            sections.add(0, newRemainder);
            return sections.stream().reduce("", String::concat);
        }
        return parseJavaFileIntoSections(sections, newRemainder.substring(newRemainder.indexOf("{") + 1, newRemainder.length()));
    }

    private int LevenshteinDistance(String task1, String task2) {
        int len0 = task1.length() + 1;
        int len1 = task2.length() + 1;
        int[] cost = new int[len0];
        int[] newCost = new int[len0];
        for (int i = 0; i < len0; i++) cost[i] = i;
        for (int j = 1; j < len1; j++) {
            newCost[0] = j;
            for (int i = 1; i < len0; i++) {
                int match = (task1.charAt(i - 1) == task2.charAt(j - 1)) ? 0 : 1;
                int cost_replace = cost[i - 1] + match;
                int cost_insert = cost[i] + 1;
                int cost_delete = newCost[i - 1] + 1;
                newCost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
            }
            int[] swap = cost;
            cost = newCost;
            newCost = swap;
        }
        return cost[len0 - 1];
    }
}
