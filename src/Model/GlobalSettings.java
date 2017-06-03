package Model;

import Controller.GoogleDriveManager;

import java.io.*;
import java.io.File;
import java.util.*;

/**
 * Created by Alexander Baranov on 03.03.2017.
 */
public class GlobalSettings implements Serializable {
    private Date editedTasksDate = new Date();


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
                        } else {
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

    public void setResultsTableURL(String resultsTableURL) {
        this.resultsTableURL = resultsTableURL;
    }

    public Date getEditedTasksDate() {
        return editedTasksDate;
    }

    void setEditedTasksDate(Date editedTasksDate) {
        this.editedTasksDate = editedTasksDate;
    }

    private HashMap<String, ArrayList<String>> subjectsAndGroups = new HashMap<>();

    public HashMap<String, ArrayList<String>> getSubjectsAndGroups() {
        return subjectsAndGroups;
    }

    // TODO Delete that block before deploy
    {
        ArrayList<String> java = new ArrayList<>(Arrays.asList("A3400", "A3401", "A3402", "A3403"));
        ArrayList<String> haskell = new ArrayList<>(Arrays.asList("A3300", "A3301", "A3400", "A3401", "A3402", "A3403"));
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


    private transient static final String hostImap = "imap.";
    private transient static final String hostSmtp = "smtp.";

    public static String getHostImap() {
        return hostImap + getEmail().split("@")[1];
    }

    public static String getHostSmtp() {
        return hostSmtp + getEmail().split("@")[1];
    }

    public static String getHostByEmail(String email) {
        return hostImap + email.split("@")[1];
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

    private String autoresponderTemplate = "";

    private String autoresponderEmailSubject = "";

    public void setAutoresponderTemplate(String autoresponderTemplate) {
        this.autoresponderTemplate = autoresponderTemplate;
    }

    public String getAutoresponderTemplate() {
        return this.autoresponderTemplate;
    }

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

    public String getAutoresponderEmailSubject() {
        return autoresponderEmailSubject;
    }

    public void setAutoresponderEmailSubject(String autoresponderEmailSubject) {
        this.autoresponderEmailSubject = autoresponderEmailSubject;
    }
}
