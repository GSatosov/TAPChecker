package Model;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.lang.reflect.Array;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by Alexander Baranov on 20.05.2017.
 */
public class LocalSettings implements Serializable {

    private static volatile LocalSettings instance;
    private List<Result> results = Collections.synchronizedList(new ArrayList<Result>());
    private Date lastDateEmailChecked = new Date(0L);
    private ArrayList<PlagiarismResult> plagiarismResults = new ArrayList<>();
    private ArrayList<Task> failedTasks = new ArrayList<>();

    private LocalSettings() {
    }

    public static LocalSettings getInstance() {
        if (instance == null) {
            synchronized (GlobalSettings.class) {
                if (instance == null) {
                    File settingsFile = new File(GlobalSettings.getDataFolder() + "/" + GlobalSettings.getLocalSettingsFileName());
                    if (settingsFile.exists()) {
                        try {
                            FileInputStream fin = new FileInputStream(GlobalSettings.getDataFolder() + "/" + GlobalSettings.getLocalSettingsFileName());
                            ObjectInputStream ois = new ObjectInputStream(fin);
                            instance = (LocalSettings) ois.readObject();
                        } catch (IOException | ClassNotFoundException e) {
                            instance = new LocalSettings();
                            settingsFile.delete();
                            e.printStackTrace();
                        }
                    } else {
                        instance = new LocalSettings();
                    }
                }
            }
        }
        return instance;
    }


    public Date getLastDateEmailChecked() {
        return this.lastDateEmailChecked;
    }

    public void setLastDateEmailChecked(Date date) {
        this.lastDateEmailChecked = date;
    }

    public ArrayList<Task> getFailedTasks() {
        return failedTasks;
    }

    public void addFailedTasks(ArrayList<Task> tasks) {
        failedTasks.addAll(tasks);
    }

    public ArrayList<PlagiarismResult> getPlagiarismResults() {
        return plagiarismResults;
    }

    public void addPlagiarismResults(ArrayList<PlagiarismResult> results) {
        this.plagiarismResults.addAll(results);
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = Collections.synchronizedList(results);
    }


    public static void saveSettings() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException {
        (new File(GlobalSettings.getDataFolder())).mkdirs();
        FileOutputStream fout = new FileOutputStream(GlobalSettings.getDataFolder() + "/" + GlobalSettings.getLocalSettingsFileName());
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(getInstance());
        oos.close();
        fout.close();
    }

}
