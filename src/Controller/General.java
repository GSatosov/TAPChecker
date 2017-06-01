package Controller;

import Model.*;
import View.MainController;
import javafx.application.Platform;

import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private static ArrayList<String> tasksThatShouldBeCheckedOnPlagiarism;

    static Date getStartDate() {
        if (startDate == null) startDate = new Date();
        return startDate;
    }

    private static void setTests(ConcurrentLinkedQueue<Task> tasks, ConcurrentHashMap<Task, ArrayList<Test>> localTests) {
        CountDownLatch latch = new CountDownLatch(tasks.size());
        tasks.forEach(task -> new Thread(() -> {
            if (localTests.containsKey(task))
                task.setTestContents(localTests.get(task));
            else
                ExponentialBackOff.execute(() -> {
                    ArrayList<Test> tests = GoogleDriveManager.getTests(task);
                    localTests.put(task, tests);
                    task.setTestContents(tests);
                    return null;
                });

            System.out.println("Tests for " + task.getSubjectName() + " " + task.getName() + " have been set: " + (new Date().getTime() - startDate.getTime()) + " ms.");
            latch.countDown();
        }).start());
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (tasks.peek().getName().endsWith("hs"))
            haskellTasksQueue.add(tasks.poll());
        else
            javaTasksQueue.add(tasks.poll());
    }

    public static void runLocalTests(Callback onExit, MainController mainController, ConcurrentLinkedQueue<Task> tasks) {
        startDate = new Date();
        haskellTasksQueue = new ConcurrentLinkedQueue<>();
        javaTasksQueue = new ConcurrentLinkedQueue<>();
        ConcurrentHashMap<Task, ArrayList<Test>> localTests = new ConcurrentHashMap<>();
        HashMap<String, ArrayList<Task>> subjectsAndTasks = LocalSettings.getInstance().getSubjectsAndTasks();
        subjectsAndTasks.keySet().forEach(key -> subjectsAndTasks.get(key).forEach(value -> {
            if (!localTests.containsKey(value))
                localTests.put(value, value.getTestContents());
        }));
        setTests(tasks, localTests);
        TestsApplier testsApplier = new TestsApplier();
        latchForTaskAppliers = new CountDownLatch(2);
        tasksThatShouldBeCheckedOnPlagiarism = new ArrayList<>();
        List<Result> results = Collections.synchronizedList(new ArrayList<Result>());
        ThreadGroup tGroup = new ThreadGroup(Thread.currentThread().getThreadGroup(), "Tasks Runners");
        haskellTasksThread(tGroup, testsApplier, results).start();
        javaTasksThread(tGroup, testsApplier, results).start();
        try {
            latchForTaskAppliers.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        saveResults(results);
        startAntiplagiarismTesting();
        checkForFailedTasks();
        try {
            LocalSettings.saveSettings();
        } catch (InvalidKeyException | NoSuchAlgorithmException | IOException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        // Sending results to Google spreadsheet
        (new Thread(new ResultsSender())).start();

        // Filling local table with results
        Platform.runLater((new Thread(() -> {
            System.out.println("Sending results to table");
            Platform.runLater((new Thread(mainController::showResults)));
        })));
        onExit.call();
    }


    private static Thread haskellTasksThread(ThreadGroup tGroup, TestsApplier applier, List<Result> results) {
        return new Thread(() -> {
            boolean startedHaskellTesting = false;
            while (tGroup.activeCount() > 0 || !haskellTasksQueue.isEmpty()) {
                if (!getHaskellTasksQueue().isEmpty()) {
                    if (!startedHaskellTesting)
                        if (!applier.startHaskellTesting())
                            break;
                    Task task = getHaskellTasksQueue().poll();
                    startedHaskellTesting = true;
                    Result haskellResult;
                    haskellResult = applier.handleHaskellTask(task);
                    if (haskellResult.getMessage().contains("OK")
                            && task.shouldBeCheckedForAntiPlagiarism()
                            && !tasksThatShouldBeCheckedOnPlagiarism.contains(task.getName()))
                        tasksThatShouldBeCheckedOnPlagiarism.add(task.getName());
                    results.add(haskellResult);
                    System.out.print(haskellResult);
                    System.out.println(" (" + ((new Date()).getTime() - General.getStartDate().getTime()) + ") ms.");
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
        });
    }

    private static Thread javaTasksThread(ThreadGroup tGroup, TestsApplier applier, List<Result> results) {
        return new Thread(() -> {
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
                            && !tasksThatShouldBeCheckedOnPlagiarism.contains(task.getName()))
                        tasksThatShouldBeCheckedOnPlagiarism.add(task.getName());
                    results.add(javaResult);
                    System.out.print(javaResult);
                    System.out.println(" (" + ((new Date()).getTime() - General.getStartDate().getTime()) + ") ms.");
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
        });
    }

    // Putting results in class system and save it
    private static void saveResults(List<Result> results) {
        LocalSettings.getInstance().getResults().addAll(results);
        List<Result> filteredResults = Collections.synchronizedList(new ArrayList<Result>());
        LocalSettings.getInstance().getResults().forEach(result -> {
            Optional<Result> firstResult = filteredResults.stream().filter(r -> r.getStudent().getName().equals(result.getStudent().getName()) && r.getTask().getName().equals(result.getTask().getName()) && r.getGroup().equals(result.getStudent().getGroupName())).findFirst();
            if (firstResult.isPresent()) {
                Result old = firstResult.get();
                if (old.compareTo(result) < 0) {
                    filteredResults.remove(old);
                    File dir = Paths.get(old.getTask().getSourcePath()).getParent().toFile();
                    if (!deleteDirectory(dir)) {
                        throw new RuntimeException("Please, delete the directory: " + dir.getAbsolutePath());
                    } else {
                        System.out.println("Result successfully deleted: " + old);
                    }
                    filteredResults.add(result);
                }
            } else {
                filteredResults.add(result);
            }
        });
        LocalSettings.getInstance().setResults(filteredResults);
        LocalSettings.getInstance().getResults().sort((r1, r2) -> r2.getTask().getReceivedDate().compareTo(r1.getTask().getReceivedDate()));
    }

    private static void checkForFailedTasks() {
        if (!haskellTasksQueue.isEmpty()) {
            addFailedTasks(new ArrayList<>(haskellTasksQueue));
            haskellTasksQueue.clear();
            System.out.println("Some of the Haskell tasks have not underwent testing for some reason. Run local tests later to re-test these tasks.");
        }
        if (!javaTasksQueue.isEmpty()) {
            addFailedTasks(new ArrayList<>(javaTasksQueue));
            javaTasksQueue.clear();
            System.out.println("Some of the Java tasks have not underwent testing for some reason. Run local tests later to re-test these tasks.");
        }
    }

    private static void addFailedTasks(ArrayList<Task> tasks) {
        ConcurrentLinkedQueue<Task> failedTasks = LocalSettings.getInstance().getFailedTasks();
        tasks.forEach(task -> {
            if (failedTasks.stream().noneMatch(task1 -> task1.getReceivedDate().getTime() == task.getReceivedDate().getTime()
                    && task.getName().equals(task1.getName()) && task.getSubjectName().equals(task1.getSubjectName())
                    && task1.getAuthor().equals(task.getAuthor())))
                failedTasks.add(task);
        });
    }

    public static void getResults(Callback onExit, MainController mainController) throws InterruptedException {
        startDate = new Date();

        latchForTaskAppliers = new CountDownLatch(2);
        ThreadGroup tGroup = new ThreadGroup(Thread.currentThread().getThreadGroup(), "Tasks Receivers");
        new Thread(tGroup, () -> {
            try {
                EmailReceiver.retrieveMessagesData();
            } catch (IOException | MessagingException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                e.printStackTrace();
            }
        }).start();

        List<Result> results = Collections.synchronizedList(new ArrayList<Result>());
        haskellTasksQueue = new ConcurrentLinkedQueue<>();
        javaTasksQueue = new ConcurrentLinkedQueue<>();
        tasksThatShouldBeCheckedOnPlagiarism = new ArrayList<>();
        {
            TestsApplier applier = new TestsApplier();
            haskellTasksThread(tGroup, applier, results).start();
            javaTasksThread(tGroup, applier, results).start();
        }
        latchForTaskAppliers.await();
        checkForFailedTasks();

        saveResults(results);
        // Plagiarism Checking
        startAntiplagiarismTesting();

        try {
            LocalSettings.saveSettings();
        } catch (InvalidKeyException | NoSuchAlgorithmException | IOException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        // Sending results to Google spreadsheet
        (new Thread(new ResultsSender())).start();

        // Filling local table with results
        Platform.runLater((new Thread(mainController::showResults)));
        onExit.call();
    }

    private static void startAntiplagiarismTesting() {
        List<ArrayList<Task>> tasksForPlagiarismCheck = Collections.synchronizedList(new ArrayList<ArrayList<Task>>());
        ArrayList<Task> tasks = LocalSettings.getInstance().getResults().stream().filter(result -> result.getMessage().contains("OK")).map(Result::getTask).collect(Collectors.toCollection(ArrayList::new));
        System.out.println("Parsing file system.");
        tasks.forEach(task -> {
            if (tasksThatShouldBeCheckedOnPlagiarism.contains(task.getName()) && tasks.stream().filter(task1 -> task1.getName().equals(task.getName())).count() > 1) {
                tasksThatShouldBeCheckedOnPlagiarism.remove(task.getName());
                tasksForPlagiarismCheck.add(tasks.stream().filter(task1 -> task1.getName().equals(task.getName())).collect(Collectors.toCollection(ArrayList::new)));
            }
        });
        PlagiarismChecker checker = new PlagiarismChecker(tasksForPlagiarismCheck, LocalSettings.getInstance().getPlagiarismResults());
        File plagiarismResultsFile = new File("data" + File.separator + "PlagiarismResults.txt");
        try {
            plagiarismResultsFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(plagiarismResultsFile));
            ArrayList<PlagiarismResult> results = checker.start();
            LocalSettings.getInstance().addPlagiarismResults(results);
            results.forEach(result -> {
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

    private static boolean deleteDirectory(File file) {
        File[] contents = file.listFiles();
        boolean flag = true;
        if (contents != null) {
            for (File f : contents) {
                if (flag) {
                    flag = deleteDirectory(f);
                }
            }
        }
        return flag && file.delete();
    }
}