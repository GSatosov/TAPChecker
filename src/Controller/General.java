package Controller;

import Model.Callback;
import Model.Result;
import Model.Task;

import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Entry point for program.
 */
public class General {
    private static void folderCleaner(String parentFolderPath) {
        File parentFolder = new File(parentFolderPath);
        String[] subFolders = parentFolder.list((dir, name) -> new File(dir, name).isDirectory());
        if (subFolders != null)
            Arrays.stream(subFolders).forEach(subFolderPath -> folderCleaner(parentFolderPath + "/" + subFolderPath));
        if (parentFolder.list() != null && parentFolder.list().length == 0)
            if (parentFolder.delete())
                System.out.println("Folder at " + parentFolderPath + " has been successfully deleted.");
    }

    private static ConcurrentLinkedQueue<Task> tasksQueue;

    static ConcurrentLinkedQueue<Task> getTasksQueue() {
        return tasksQueue;
    }

    private static Date startDate;

    static Date getStartDate() {
        if (startDate == null) startDate = new Date();
        return startDate;
    }

    public static void getResults(Callback callback) {
        startDate = new Date();
        ArrayList<Result> results = new ArrayList<>();
        tasksQueue = new ConcurrentLinkedQueue<>();
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
            while (tGroup.activeCount() > 0 || !getTasksQueue().isEmpty()) {
                if (!getTasksQueue().isEmpty()) {
                    Task task = getTasksQueue().poll();
                    if (task.getName().endsWith("hs")) {
                        if (!applier.sentHaskellTasks())
                            applier.startHaskellProcess();
                        Result resultHaskell = applier.handleHaskellTask(task);
                        results.add(resultHaskell);
                        System.out.println(resultHaskell);
                    } else {
                        if (!applier.sentJavaTasks())
                            applier.startJavaTesting();
                        Result resultJava = applier.handleJavaTask(task);
                        results.add(resultJava);
                        System.out.println(resultJava);
                    }
                } else {
                    try {
                        //if (tGroup != null) System.out.println(tGroup.activeCount());
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (applier.sentHaskellTasks())
                applier.finishHaskellTesting();
            System.out.println(new Date().getTime() - startDate.getTime() + " ms.");
            System.out.println("Task applier closed.");
            System.out.println("Running thread for results sender...");
            callback.call();
            (new Thread(new ResultsSender(results))).start();
        })).start();
    }

}
