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

    private static void setTests(ConcurrentLinkedQueue<Task> tasks, ConcurrentHashMap<Task, ArrayList<Test>> localTests, CountDownLatch setTestsLatch) {
        CountDownLatch latch = new CountDownLatch(tasks.size());
        tasks.forEach(task -> new Thread(() -> {
            if (localTests.containsKey(task)) {
                task.setTestContents(localTests.get(task));
                latch.countDown();
                MainController.println("Tests for " + task.getSubjectName() + " " + task.getName() + " have been set: " + (new Date().getTime() - startDate.getTime()) + " ms.");
            } else {
                ExponentialBackOff.execute(() -> {
                    ArrayList<Test> tests = GoogleDriveManager.getTests(task);
                    localTests.put(task, tests);
                    task.setTestContents(tests);
                    latch.countDown();
                    MainController.println("Tests for " + task.getSubjectName() + " " + task.getName() + " have been set: " + (new Date().getTime() - startDate.getTime()) + " ms.");
                    return null;
                });
            }
            if (LocalSettings.getInstance().getSubjectsAndTasks().size() != 0)
                if (task.getName().contains("."))
                    task.setAdditionalTest(LocalSettings.getInstance().getSubjectsAndTasks().get(task.getSubjectName().replaceAll("_", " ")).stream().filter(task1 -> task1.getName().equals(task.getName().split("\\.")[0])).findFirst().get().getAdditionalTest());
                else
                    task.setAdditionalTest(LocalSettings.getInstance().getSubjectsAndTasks().get(task.getSubjectName().replaceAll("_", " ")).stream().filter(task1 -> task1.getName().equals(task.getName())).findFirst().get().getAdditionalTest());
        }).start());
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (tasks.size() > 0) {
            if (tasks.peek().getName().endsWith("hs"))
                haskellTasksQueue.add(tasks.poll());
            else
                javaTasksQueue.add(tasks.poll());
        }
        setTestsLatch.countDown();
    }

    public static void runLocalTests(Callback onExit, MainController mainController, ConcurrentLinkedQueue<Task> tasks) {
        startDate = new Date();
        haskellTasksQueue = new ConcurrentLinkedQueue<>();
        javaTasksQueue = new ConcurrentLinkedQueue<>();
        ConcurrentHashMap<Task, ArrayList<Test>> localTests = new ConcurrentHashMap<>();
        HashMap<String, ArrayList<Task>> subjectsAndTasks = LocalSettings.getInstance().getSubjectsAndTasks();
        subjectsAndTasks.values().forEach(value -> value.forEach(v -> {
            if (!localTests.containsKey(v))
                localTests.put(v, v.getTestContents());
        }));

        CountDownLatch setTestsLatch = new CountDownLatch(1);

        // setTests
        new Thread(() -> {
            setTests(tasks, localTests, setTestsLatch);
        }).start();

        // test appliers
        new Thread(() -> {
            try {
                setTestsLatch.await();
                TestsApplier testsApplier = new TestsApplier();
                tasksThatShouldBeCheckedOnPlagiarism = new ArrayList<>();
                List<Result> results = Collections.synchronizedList(new ArrayList<Result>());

                latchForTaskAppliers = new CountDownLatch(2);

                ThreadGroup tGroup = new ThreadGroup(Thread.currentThread().getThreadGroup(), "Tasks Runners");
                haskellTasksThread(tGroup, testsApplier, results).start();
                javaTasksThread(tGroup, testsApplier, results).start();
                resultsHandler(onExit, mainController, results).start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

    }

    private static Thread resultsHandler(Callback onExit, MainController mainController, List<Result> results) {
        return new Thread(() -> {
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
            Platform.runLater((new Thread(mainController::showResults)));

            onExit.call();

        });
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
                    MainController.println(haskellResult.toString() + " (" + ((new Date()).getTime() - General.getStartDate().getTime()) + ") ms.");
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
            MainController.println("Haskell task applier closed: " + (new Date().getTime() - startDate.getTime() + " ms."));
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
                    MainController.println(javaResult.toString() + " (" + ((new Date()).getTime() - General.getStartDate().getTime()) + ") ms.");
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            MainController.println("Java task applier closed: " + (new Date().getTime() - startDate.getTime() + " ms."));
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
                    if (!old.getTask().getSourcePath().equals(result.getTask().getSourcePath())) {
                        File dir = Paths.get(old.getTask().getSourcePath()).getParent().toFile();
                        if (!deleteDirectory(dir)) {
                            throw new RuntimeException("Please, delete the directory: " + dir.getAbsolutePath());
                        } else {
                            MainController.println("Result successfully deleted: " + old);
                        }
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
            MainController.println("Some of the Haskell tasks have not underwent testing for some reason. Run failed tasks later to re-test these tasks.");
        }
        if (!javaTasksQueue.isEmpty()) {
            addFailedTasks(new ArrayList<>(javaTasksQueue));
            javaTasksQueue.clear();
            MainController.println("Some of the Java tasks have not underwent testing for some reason. Run failed tasks later to re-test these tasks.");
        }
    }

    private static void addFailedTasks(ArrayList<Task> tasks) {
        ConcurrentLinkedQueue<Task> failedTasks = LocalSettings.getInstance().getFailedTasks();
        tasks.forEach(task -> {
            if (failedTasks.stream().noneMatch(task1 -> task1.getReceivedDate().getTime() == task.getReceivedDate().getTime()
                    && task.equals(task1) && task1.getAuthor().equals(task.getAuthor())))
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

        TestsApplier applier = new TestsApplier();
        haskellTasksThread(tGroup, applier, results).start();
        javaTasksThread(tGroup, applier, results).start();
        resultsHandler(onExit, mainController, results).start();
    }

    private static void startAntiplagiarismTesting() {
        List<ArrayList<Task>> tasksForPlagiarismCheck = Collections.synchronizedList(new ArrayList<ArrayList<Task>>());
        ArrayList<Task> tasks = LocalSettings.getInstance().getResults().stream().filter(result -> result.getMessage().contains("OK")).map(Result::getTask).collect(Collectors.toCollection(ArrayList::new));
        MainController.println("Parsing file system.");
        tasks.forEach(task -> {
            if (tasksThatShouldBeCheckedOnPlagiarism.contains(task.getName()) && tasks.stream().filter(task::equals).count() > 1) {
                tasksThatShouldBeCheckedOnPlagiarism.remove(task.getName());
                tasksForPlagiarismCheck.add(tasks.stream().filter(task::equals).collect(Collectors.toCollection(ArrayList::new)));
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
        MainController.println("Plagiarism check has been concluded.");
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

    public static void logList(BufferedWriter writer, List<String> list) {
        list.forEach(line -> {
            try {
                writer.write(line);
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}