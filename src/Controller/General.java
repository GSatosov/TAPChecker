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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

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

    public static void getResults(Callback callback) {
        try {
            GoogleDriveManager.authorize();
        } catch (IOException e) {
            callback.call();
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
                    Result resultHaskell = applier.handleHaskellTask(task);
                    results.add(resultHaskell);
                    System.out.println(resultHaskell);
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
                    Result resultJava = applier.handleJavaTask(task);
                    results.add(resultJava);
                    System.out.println(resultJava);
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
                ArrayList<Task> classSystem = new ArrayList<>();
                (new Thread(new ResultsSender(results, callback, classSystem))).start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        })).start();
    }

}
