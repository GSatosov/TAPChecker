package Model;

import java.io.File;

/**
 * Created by Alexander Baranov on 03.03.2017.
 */
public class Constants {

    /*
    Host address
     */
    public static final String HOST = "imap.gmail.com";
    /*
    E-mail address
     */
    public static final String EMAIL = "kubenskiythesis@gmail.com";
    /*
    Password
     */
    public static final String PASSWORD = "sansanich";

    /*
    Folder path for all attachments which we receive from e-mail
     */
    public static final String DATA_FOLDER = "data";

    /*
    Application name.
     */
    public static final String APPLICATION_NAME = "Thesis";

    /*
    Directory to store user credentials for this application.
     */
    public static final File CREDENTIALS_STORE_DIR = new File(System.getProperty("user.home"), ".credentials/thesis");

}
