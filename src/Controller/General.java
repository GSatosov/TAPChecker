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

    public static List<Result> getResults() throws MessagingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException {
        Date startDate = new Date();
        ArrayList<Task> tasks = EmailReceiver.retrieveMessagesData();
        Date taskDate = new Date();
        System.out.println((taskDate.getTime() - startDate.getTime()) + " ms to get the tasks.");
        HashMap<String, ArrayList<Test>> localTests = new HashMap<>();
        ArrayList<Task> javaTasks = new ArrayList<>();
        ArrayList<Task> haskellTasks = new ArrayList<>();
        tasks.forEach(task -> {
            if (localTests.containsKey(task.getName())) {
                task.setTestContents(localTests.get(task.getName()));
            }
            try {
                ArrayList<Test> curTests = GoogleDriveManager.getTests(task);
                if (curTests != null) System.out.println(curTests);
                localTests.put(task.getName(), curTests);
                task.setTestContents(curTests);
                if (task.getName().endsWith("hs"))
                    haskellTasks.add(task);
                else
                    javaTasks.add(task);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        Date testDate = new Date();
        System.out.println((testDate.getTime() - taskDate.getTime()) + " ms to get the tests.");
        TestsApplier applier = new TestsApplier();
        ArrayList<Result> results = applier.applyHaskellTests(haskellTasks);
        results.addAll((applier.applyJavaTests(javaTasks)));
        System.out.println((new Date().getTime() - testDate.getTime()) + " ms to run the tests.");
        //  folderCleaner("data");
        return results;
    }

    public static void main(String[] args) {
        try {
            getResults().forEach(System.out::println);
        } catch (MessagingException | NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }
}
