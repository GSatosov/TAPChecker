package Model;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Created by Alexander Baranov on 03.03.2017.
 */
public class Settings implements Serializable {

    private static volatile Settings instance;

    private Settings() {
    }

    public static Settings getInstance() {
        if (instance == null) {
            synchronized (Settings.class) {
                if (instance == null) {
                    File settingsFile = new File(getDataFolder() + "/" + getSettingsFileName());
                    if (settingsFile.exists()) {
                        try {
                            FileInputStream fin = new FileInputStream(getDataFolder() + "/" + getSettingsFileName());
                            ObjectInputStream ois = new ObjectInputStream(fin);
                            instance = (Settings) ois.readObject();
                        } catch (IOException | ClassNotFoundException e) {
                            instance = new Settings();
                            e.printStackTrace();
                        }
                    } else {
                        instance = new Settings();
                    }
                }
            }
        }
        return instance;
    }

    private String resultsTableURL = "1cIg4hbegIKJfiy7-CM0lzXqRss8AgyxYlWFGT3hxZE8";

    public String getResultsTableURL() {
        return resultsTableURL;
    }

    /*
    Host address
     */
    private transient static final String host = "imap.";
    /*
    E-mail address
     */
    private transient static String email = "kubenskiythesis@gmail.com"; //kubenskiythesis@gmail.com
    /*
    Password
     */
    private transient static String password = "sansanich"; //sansanich

    /*
    Folder path for all attachments which we receive from e-mail
     */
    private transient static final String dataFolder = "data";

    /*
    Application name.
     */
    private transient static final String applicationName = "Thesis";

    /*
    Directory to store user credentials for this application.
     */
    private transient static final File credentialsStoreDir = new File(System.getProperty("user.home"), ".credentials/" + getApplicationName() + "/google");

    private transient static final String sourcesDateFormat = "yyyyMMddHHmmss";

    public static String getSourcesDateFormat() {
        return sourcesDateFormat;
    }

    /*
    Last date when email was checked.
     */
    private Date lastDateEmailChecked = new Date(0L);

    private transient static final String clientId = "917113484141-sbljoi0kv8bbto6hj120esjg6a5dv4ur.apps.googleusercontent.com";

    private transient static final String clientSecret = "scbK-8lyW9fNT8SluhGLu8i6";

    private transient static final String settingsFileName = "settings.dat";

    public static String getClientId() {
        return clientId;
    }

    public static String getClientSecret() {
        return clientSecret;
    }

    public static void setEmail(String email) {
        Settings.email = email;
    }

    public static void setPassword(String password) {
        Settings.password = password;
    }

    private static String getSettingsFileName() {
        return settingsFileName;
    }

    public String getHost() {
        return host + getEmail().split("@")[1];
    }

    public static String getHostByEmail(String email) {
        return host + email.split("@")[1];
    }


    public static String getEmail() {
        return email;
    }

    public static String getPassword() {
        return password;
    }

    public static String getDataFolder() {
        return dataFolder;
    }

    public static String getApplicationName() {
        return applicationName;
    }

    public static File getCredentialsStoreDir() {
        return credentialsStoreDir;
    }

    public Date getLastDateEmailChecked() {
        return this.lastDateEmailChecked;
    }

    public void setLastDateEmailChecked(Date date) {
        this.lastDateEmailChecked = date;
    }

    /*
    Encrypt and save settings file to data folder.
     */
    public void saveSettings() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException {
        (new File(Settings.getDataFolder())).mkdirs();
        FileOutputStream fout = new FileOutputStream(getDataFolder() + "/" + getSettingsFileName());
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(getInstance());
        oos.close();
        fout.close();
    }

}
