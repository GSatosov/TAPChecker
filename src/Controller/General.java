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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

    private static ArrayList<Task> getTasks() throws MessagingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException {
        ArrayList<Task> tasks = EmailReceiver.retrieveMessagesData();
        HashMap<String, ArrayList<Test>> localTests = new HashMap<>();
        tasks.forEach(task -> {
            if (localTests.containsKey(task.getName())) {
                task.setTestContents(localTests.get(task.getName()));
            }
            try {
                ArrayList<Test> curTests = GoogleDriveManager.getTests(task);
                if (curTests != null) System.out.println(curTests);
                localTests.put(task.getName(), curTests);
                task.setTestContents(curTests);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return tasks;
    }

    private static List<Result> applyTests(ArrayList<Task> tasks) throws IOException, InterruptedException {
        TestsApplier applier = new TestsApplier();
        ArrayList<Task> javaTasks = new ArrayList<>();
        ArrayList<Task> haskellTasks = new ArrayList<>();
        tasks.forEach(task -> {
            if (task.getName().endsWith("hs"))
                haskellTasks.add(task);
            else
                javaTasks.add(task);
        });
        List<Result> results = applier.applyHaskellTests(haskellTasks);
        results.addAll((applier.applyJavaTests(javaTasks)));
        return results;
    }

    public static void main(String[] args) {
        try {
            applyTests(getTasks()).forEach(System.out::println);
        } catch (MessagingException | NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        folderCleaner("data");
    }
}
