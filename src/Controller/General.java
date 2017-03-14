package Controller;

import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Alexander Baranov on 03.03.2017.
 */
public class General {

    public static void main(String[] args) {
        try {
            EmailReceiver.retrieveMessagesData();
        }
        catch (IOException | MessagingException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }
}
