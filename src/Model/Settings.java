package Model;

import Controller.Cryptographer;
import com.google.api.client.util.IOUtils;

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
                            FileInputStream inputStream = new FileInputStream(settingsFile);
                            instance = (Settings)Cryptographer.decrypt(inputStream);
                        }
                        catch (IOException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
                            instance = new Settings();
                            e.printStackTrace();
                        }
                    }
                    else {
                        instance = new Settings();
                    }
                }
            }
        }
        return instance;
    }

    /*
    Host address
     */
    private String host = "imap.gmail.com";
    /*
    E-mail address
     */
    private String email = "kubenskiythesis@gmail.com";
    /*
    Password
     */
    private String password = "sansanich";

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
    private transient static final File credentialsStoreDir = new File(System.getProperty("user.home"), ".credentials/thesis");

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

    public static String getSettingsFileName() {
        return settingsFileName;
    }

    public String getHost() {
        return this.host;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPassword() {
        return this.password;
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Cryptographer.encrypt(getInstance(), baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        IOUtils.copy(bais, new FileOutputStream(getDataFolder() + "/" + getSettingsFileName()));
    }

}
