package Controller;

import javax.mail.MessagingException;
import java.io.IOException;

/**
 * Created by Alexander Baranov on 03.03.2017.
 */
public class General {

    public static void main(String[] args) {
        try {
            EmailReceiver.retrieveMessagesData();
        }
        catch (IOException | MessagingException e) {
            e.printStackTrace();
        }
    }
}
