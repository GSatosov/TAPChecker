package Model;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by Alexander Baranov on 20.05.2017.
 */
public class LocalSettings implements Serializable {

    private static volatile LocalSettings instance;

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


    private Date lastDateEmailChecked = new Date(0L);

    public Date getLastDateEmailChecked() {
        return this.lastDateEmailChecked;
    }

    public void setLastDateEmailChecked(Date date) {
        this.lastDateEmailChecked = date;
    }


    private List<Result> results = Collections.synchronizedList(new ArrayList<Result>());

    public List<Result> getResults() {
        return results;
    }


    private HashMap<String, ArrayList<String>> subjectsAndGroups = new HashMap<>();

    public HashMap<String, ArrayList<String>> getSubjectsAndGroups() {
        return subjectsAndGroups;
    }


    public void saveSettings() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException {
        (new File(GlobalSettings.getDataFolder())).mkdirs();
        FileOutputStream fout = new FileOutputStream(GlobalSettings.getDataFolder() + "/" + GlobalSettings.getLocalSettingsFileName());
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(getInstance());
        oos.close();
        fout.close();
    }

}
