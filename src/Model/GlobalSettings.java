package Model;

import Controller.GoogleDriveManager;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.model.*;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

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
                    try {
                        ByteArrayOutputStream outputStream = GoogleDriveManager.getGlobalSettings();
                        if (outputStream != null) {
                            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(outputStream.toByteArray());
                            ObjectInput in = new ObjectInputStream(byteArrayInputStream);
                            instance = (GlobalSettings) in.readObject();
                        }
                        else {
                            instance = new GlobalSettings();
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        instance = new GlobalSettings();
                        e.printStackTrace();
                    }
                }
            }
        }
        return instance;
    }

    /*
    Serializable variables
     */

    private String resultsTableURL = "https://docs.google.com/spreadsheets/d/1cIg4hbegIKJfiy7-CM0lzXqRss8AgyxYlWFGT3hxZE8/edit#gid=0";

    public String getResultsTableURL() {
        return resultsTableURL;
    }


    private HashMap<String, List<String>> subjectsAndGroups = new HashMap<>();

    public HashMap<String, List<String>> getSubjectsAndGroups() {
        return subjectsAndGroups;
    }

    // TODO Delete that block
    {
        List<String> java = Arrays.asList("A3400", "A3401", "A3402", "A3403");
        List<String> haskell = Arrays.asList("A3300", "A3301", "A3400", "A3401", "A3402", "A3403");
        getSubjectsAndGroups().put("Java", java);
        getSubjectsAndGroups().put("Функциональное программирование", haskell);
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

    public static String getGlobalSettingsFileName() {
        return globalSettingsFileName;
    }

    public static void saveFile() throws IOException {
        (new File(GlobalSettings.getDataFolder())).mkdirs();
        FileOutputStream fout = new FileOutputStream(getDataFolder() + "/" + getGlobalSettingsFileName());
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(getInstance());
        oos.close();
        fout.close();

        GoogleDriveManager.saveGlobalSettings();

        File globalSettings = new File(getDataFolder() + "/" + getGlobalSettingsFileName());
        globalSettings.delete();

    }

}
