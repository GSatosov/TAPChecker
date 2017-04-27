package Controller;

import Model.Result;
import Model.Task;
import Model.Test;

import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    public static ConcurrentLinkedQueue<Task> getTasksQueue() {
        return tasksQueue;
    }
    private static Date startDate = new Date();
    public static Date getStartDate() {
        return startDate;
    }

    public static void getResults() {
        tasksQueue = new ConcurrentLinkedQueue<Task>();
        TestsApplier applier = new TestsApplier();
        ThreadGroup tGroup = new ThreadGroup(Thread.currentThread().getThreadGroup(), "Tasks Receivers");
        Thread taskReceiver = new Thread(tGroup, () -> {
            try {
                EmailReceiver.retrieveMessagesData();
            } catch (IOException | MessagingException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
                e.printStackTrace();
            }
        });
        taskReceiver.start();
        (new Thread(() -> {
            while (tGroup.activeCount() > 0 || !getTasksQueue().isEmpty()) {
                if (!getTasksQueue().isEmpty()) {
                    ArrayList<Task> task = new ArrayList<>();
                    task.add(getTasksQueue().peek());
                    if (task.get(0).getName().endsWith("hs")) {
                        System.out.println(applier.applyHaskellTests(task) + " (" + ((new Date()).getTime() - getStartDate().getTime()) + " s.)");
                    }
                    else {
                        System.out.println(applier.applyJavaTests(task) + " (" + ((new Date()).getTime() - getStartDate().getTime()) + " s.)");
                    }
                    getTasksQueue().remove();
                }
                else {
                    try {
                        //if (tGroup != null) System.out.println(tGroup.activeCount());
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Task applier closed.");
        })).start();
    }

    public static void main(String[] args) {
        getResults();
    }
}
