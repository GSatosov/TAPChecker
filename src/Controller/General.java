package Controller;

import Model.Task;

import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Created by Alexander Baranov on 03.03.2017.
 */
public class General {

    public static void main(String[] args) {
        try {
            ArrayList<Task> tasks = EmailReceiver.retrieveMessagesData();
            System.out.println("Tasks: " + tasks);
        }
        catch (IOException | MessagingException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }
}
