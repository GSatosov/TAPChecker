package Controller;

import Model.Task;

import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Alexander Baranov on 03.03.2017.
 */
public class General {

    public static void main(String[] args) {
        try {
            ArrayList<Task> tasks = EmailReceiver.retrieveMessagesData();
            TestsApplier applier = new TestsApplier();
            HashMap<String, HashMap<String, ArrayList<String>>> tests = new HashMap<>();
            tasks.stream().map(task -> {
                if (tests.containsKey(task.getName())) {
                    task.setTestContents(tests.get(task.getName()));
                    return task;
                }
                try {
                    HashMap<String, ArrayList<String>> curTests = GoogleSheetsManager.getTests(task);
                    tests.put(task.getName(), curTests);
                    task.setTestContents(curTests);
                    return task;
                } catch (IOException e) {
                    e.printStackTrace();
                    return task;
                }
            }).forEach(task -> {
                try {
                    System.out.println(applier.applyTests(task));
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        } catch (MessagingException | NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException |
                IOException e) {
            e.printStackTrace();
        }
    }
}
