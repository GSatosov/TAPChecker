package Controller;

import Model.Callback;
import Model.Result;
import Model.Task;

import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Entry point for program.
 */
public class General {

    private static ConcurrentLinkedQueue<Task> haskellTasksQueue;
    private static ConcurrentLinkedQueue<Task> javaTasksQueue;
    private static CountDownLatch latch;

    static ConcurrentLinkedQueue<Task> getHaskellTasksQueue() {
        return haskellTasksQueue;
    }

    static ConcurrentLinkedQueue<Task> getJavaTasksQueue() {
        return javaTasksQueue;
    }

    private static Date startDate;

    static Date getStartDate() {
        if (startDate == null) startDate = new Date();
        return startDate;
    }

    public static void getResults(Callback onExit) {
        try {
            GoogleDriveManager.authorize();
        } catch (IOException e) {
            onExit.call();
            return;
        }
        latch = new CountDownLatch(2);
        startDate = new Date();
        ArrayList<Result> results = new ArrayList<>();
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
            while (tGroup.activeCount() > 0 || !haskellTasksQueue.isEmpty()) {
                if (!getHaskellTasksQueue().isEmpty()) {
                    Task task = getHaskellTasksQueue().poll();
                    if (!applier.startedHaskellTesting())
                        applier.startHaskellTesting();
                    Result haskellResult;
                    if (task.getReceivedDate().getTime() > task.getTestContents().get(0).getDeadline().getTime() && task.getTestContents().get(0).hasHardDeadline())
                        haskellResult = new Result("DL", task);
                    else
                        haskellResult = applier.handleHaskellTask(task);
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
            if (applier.startedHaskellTesting())
                applier.finishHaskellTesting();
            System.out.println("Haskell task applier closed: " + (new Date().getTime() - startDate.getTime() + " ms."));
            latch.countDown();
        })).start();
        (new Thread(() -> {
            while (tGroup.activeCount() > 0 || !javaTasksQueue.isEmpty()) {
                if (!getJavaTasksQueue().isEmpty()) {
                    Task task = getJavaTasksQueue().poll();
                    if (!applier.startedJavaTesting())
                        applier.startJavaTesting();
                    Result javaResult;
                    if (task.getReceivedDate().getTime() > task.getTestContents().get(0).getDeadline().getTime() && task.getTestContents().get(0).hasHardDeadline())
                        javaResult = new Result("DL", task);
                    else
                        javaResult = applier.handleJavaTask(task);
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
            latch.countDown();
        })).start();
        (new Thread(() -> {
            try {
                latch.await();
                System.out.println("Running thread for results sender...");
                List<Result> classSystem = Collections.synchronizedList(new ArrayList<Result>());
                (new Thread(new ResultsSender(results, onExit, classSystem, () -> startAntiplagiarismTesting(classSystem)))).start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        })).start();
    }

    private static void startAntiplagiarismTesting(List<Result> classSystem) {
        ArrayList<String> taskNames = new ArrayList<>();
        List<ArrayList<Task>> tasksForPlagiarismCheck = Collections.synchronizedList(new ArrayList<ArrayList<Task>>());
        ArrayList<Task> tasks = classSystem.stream().filter(result -> result.getMessage().contains("OK")).map(Result::getTask).collect(Collectors.toCollection(ArrayList::new));
        System.out.println("Parsing file system.");
        tasks.forEach(task -> {
            if (!taskNames.contains(task.getName()) && tasks.stream().filter(task1 -> task1.getName().equals(task.getName())).count() > 1) {
                taskNames.add(task.getName());
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