package Controller;

import Model.Task;

import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Entry point for program.
 */
public class General {

    public static void main() {
        try {
            ArrayList<Task> tasks = EmailReceiver.retrieveMessagesData();
            TestsApplier applier = new TestsApplier();
            HashMap<String, HashMap<String, ArrayList<String>>> tests = new HashMap<>();
            tasks.forEach(task -> {
                if (tests.containsKey(task.getName())) {
                    task.setTestContents(tests.get(task.getName()));
                }
                try {
                    HashMap<String, ArrayList<String>> curTests = GoogleSheetsManager.getTests(task);
                    tests.put(task.getName(), curTests);
                    task.setTestContents(curTests);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            applier.applyTests(tasks).forEach(System.out::println);
        } catch (MessagingException | NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
