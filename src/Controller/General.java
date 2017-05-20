package Controller;

import Model.Callback;
import Model.LocalSettings;
import Model.Result;
import Model.Task;
import View.MainController;
import javafx.application.Platform;

import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Entry point for program.
 */
public class General {

    private static ConcurrentLinkedQueue<Task> haskellTasksQueue;
    private static ConcurrentLinkedQueue<Task> javaTasksQueue;
    private static CountDownLatch latchForTaskAppliers;

    static ConcurrentLinkedQueue<Task> getHaskellTasksQueue() {
        return haskellTasksQueue;
    }

    static ConcurrentLinkedQueue<Task> getJavaTasksQueue() {
        return javaTasksQueue;
    }

    private static Date startDate;
    private static ArrayList<String> tasksThatShouldBeCheckedOnAntiPlagiarism;

    static Date getStartDate() {
        if (startDate == null) startDate = new Date();
        return startDate;
    }

    public static void getResults(Callback onExit, MainController mainController) throws InterruptedException {
        try {
            GoogleDriveManager.authorize();
        } catch (IOException e) {
            onExit.call();
            return;
        }
        tasksThatShouldBeCheckedOnAntiPlagiarism = new ArrayList<>();
        latchForTaskAppliers = new CountDownLatch(2);
        startDate = new Date();
        List<Result> results = Collections.synchronizedList(new ArrayList<Result>());
        haskellTasksQueue = new ConcurrentLinkedQueue<>();
        javaTasksQueue = new ConcurrentLinkedQueue<>();
        TestsApplier applier = new TestsApplier();
        ThreadGroup tGroup = new ThreadGroup(Thread.currentThread().getThreadGroup(), "Tasks Receivers");
        (new Thread(tGroup, () -> {
            try {
                EmailReceiver.retrieveMessagesData();
            } catch (IOException | MessagingException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                e.printStackTrace();
            }
        })).start();

        (new Thread(() -> {
            boolean startedHaskellTesting = false;
            while (tGroup.activeCount() > 0 || !haskellTasksQueue.isEmpty()) {
                if (!getHaskellTasksQueue().isEmpty()) {
                    Task task = getHaskellTasksQueue().poll();
                    if (!startedHaskellTesting)
                        if (!applier.startHaskellTesting())
                            break;
                    startedHaskellTesting = true;
                    Result haskellResult;
                        haskellResult = applier.handleHaskellTask(task);
                    if (haskellResult.getMessage().contains("OK")
                            && task.shouldBeCheckedForAntiPlagiarism()
                            && !tasksThatShouldBeCheckedOnAntiPlagiarism.contains(task.getName()))
                        tasksThatShouldBeCheckedOnAntiPlagiarism.add(task.getName());
                    results.add(haskellResult);
                    System.out.println(haskellResult);
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (startedHaskellTesting)
                applier.finishHaskellTesting();
            System.out.println("Haskell task applier closed: " + (new Date().getTime() - startDate.getTime() + " ms."));
            latchForTaskAppliers.countDown();
        })).start();
        (new Thread(() -> {
            boolean startedJavaTesting = false;
            while (tGroup.activeCount() > 0 || !javaTasksQueue.isEmpty()) {
                if (!getJavaTasksQueue().isEmpty()) {
                    Task task = getJavaTasksQueue().poll();
                    if (!startedJavaTesting) {
                        applier.startJavaTesting();
                        startedJavaTesting = true;
                    }
                    Result javaResult;
                        javaResult = applier.handleJavaTask(task);
                    if (javaResult.getMessage().contains("OK")
                            && task.shouldBeCheckedForAntiPlagiarism()
                            && !tasksThatShouldBeCheckedOnAntiPlagiarism.contains(task.getName()))
                        tasksThatShouldBeCheckedOnAntiPlagiarism.add(task.getName());
                    results.add(javaResult);
                    System.out.println(javaResult);
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Java task applier closed: " + (new Date().getTime() - startDate.getTime() + " ms."));
            latchForTaskAppliers.countDown();
        })).start();

        latchForTaskAppliers.await();

        LocalSettings.getInstance().getResults().addAll(results);
        startAntiplagiarismTesting();

        CountDownLatch tableLatch = new CountDownLatch(1);
        (new Thread(() -> {
            System.out.println("Running thread for results sender...");
            (new Thread(new ResultsSender(results))).start();
        })).start();
        Platform.runLater(() -> {
            try {
                tableLatch.await();
                System.out.println("Sending results to table");
                Platform.runLater((new Thread(() -> {
                    mainController.showResults(results);
                })));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        tableLatch.countDown();
        onExit.call();
    }

    private static void startAntiplagiarismTesting() {
        List<ArrayList<Task>> tasksForPlagiarismCheck = Collections.synchronizedList(new ArrayList<ArrayList<Task>>());
        ArrayList<Task> tasks = LocalSettings.getInstance().getResults().stream().filter(result -> result.getMessage().contains("OK")).map(Result::getTask).collect(Collectors.toCollection(ArrayList::new));
        System.out.println("Parsing file system.");
        tasks.forEach(task -> {
            if (tasksThatShouldBeCheckedOnAntiPlagiarism.contains(task.getName()) && tasks.stream().filter(task1 -> task1.getName().equals(task.getName())).count() > 1) {
                tasksThatShouldBeCheckedOnAntiPlagiarism.remove(task.getName());
                tasksForPlagiarismCheck.add(tasks.stream().filter(task1 -> task1.getName().equals(task.getName())).collect(Collectors.toCollection(ArrayList::new)));
            }
        });
        PlagiarismChecker checker = new PlagiarismChecker(tasksForPlagiarismCheck);
        File plagiarismResultsFile = new File("PlagiarismResults.txt");
        try {
            plagiarismResultsFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(plagiarismResultsFile));
            checker.start().forEach(result -> {
                try {
                    writer.write(result.toString());
                    writer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Plagiarism check has been concluded. The results lie in PlagiarismResult.txt file.");
    }
}