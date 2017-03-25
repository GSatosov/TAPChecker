package Controller;

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

    public static void main(String[] args) {
        try {
            ArrayList<Task> tasks = EmailReceiver.retrieveMessagesData();
            TestsApplier applier = new TestsApplier();
            HashMap<String, ArrayList<Test>> localTests = new HashMap<>();
            tasks.forEach(task -> {
                if (localTests.containsKey(task.getName())) {
                    task.setTestContents(localTests.get(task.getName()));
                }
                try {
                    ArrayList<Test> curTests = GoogleSheetsManager.getTests(task);
                    localTests.put(task.getName(), curTests);
                    task.setTestContents(curTests);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            applier.applyHaskellTests(tasks).forEach(System.out::println);
        } catch (MessagingException | NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        folderCleaner("data");
    }
}
