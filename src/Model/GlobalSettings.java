package Model;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Created by Alexander Baranov on 03.03.2017.
 */
public class GlobalSettings implements Serializable {

    private static volatile GlobalSettings instance;
    private GlobalSettings() {
    }
    public static GlobalSettings getInstance() {
        if (instance == null) {
            synchronized (GlobalSettings.class) {
                if (instance == null) {
                    File settingsFile = new File(getDataFolder() + "/" + getGlobalSettingsFileName());
                    if (settingsFile.exists()) {
                        try {
                            FileInputStream fin = new FileInputStream(getDataFolder() + "/" + getGlobalSettingsFileName());
                            ObjectInputStream ois = new ObjectInputStream(fin);
                            instance = (GlobalSettings) ois.readObject();
                        } catch (IOException | ClassNotFoundException e) {
                            instance = new GlobalSettings();
                            e.printStackTrace();
                        }
                    } else {
                        instance = new GlobalSettings();
                    }
                }
            }
        }
        return instance;
    }

    /*
    Serializable variables
     */

    private String resultsTableURL = "1cIg4hbegIKJfiy7-CM0lzXqRss8AgyxYlWFGT3hxZE8";

    public String getResultsTableURL() {
        return resultsTableURL;
    }


    /*
    Transient variables
     */

    private transient static final String localSettingsFileName = "localSettings.dat";

    public static String getLocalSettingsFileName() {
        return localSettingsFileName;
    }


    private transient static final String host = "imap.";

    public static String getHost() {
        return host + getEmail().split("@")[1];
    }

    public static String getHostByEmail(String email) {
        return host + email.split("@")[1];
    }


    private transient static String email = "kubenskiythesis@gmail.com"; //kubenskiythesis@gmail.com

    public static String getEmail() {
        return email;
    }

    public static void setEmail(String email) {
        GlobalSettings.email = email;
    }


    private transient static String password = "sansanich"; //sansanich

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        GlobalSettings.password = password;
    }


    private transient static final String dataFolder = "data";

    public static String getDataFolder() {
        return dataFolder;
    }


    private transient static final String applicationName = "Thesis";

    public static String getApplicationName() {
        return applicationName;
    }


    private transient static final File credentialsStoreDir = new File(System.getProperty("user.home"), ".credentials/" + getApplicationName() + "/google");

    public static File getCredentialsStoreDir() {
        return credentialsStoreDir;
    }


    private transient static final String sourcesDateFormat = "yyyyMMddHHmmss";

    public static String getSourcesDateFormat() {
        return sourcesDateFormat;
    }


    private transient static final String clientId = "917113484141-sbljoi0kv8bbto6hj120esjg6a5dv4ur.apps.googleusercontent.com";

    public static String getClientId() {
        return clientId;
    }


    private transient static final String clientSecret = "scbK-8lyW9fNT8SluhGLu8i6";

    public static String getClientSecret() {
        return clientSecret;
    }


    private transient static final String globalSettingsFileName = "globalSettings.dat";

    private static String getGlobalSettingsFileName() {
        return globalSettingsFileName;
    }

}
