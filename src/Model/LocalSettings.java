package Model;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Alexander Baranov on 20.05.2017.
 */
public class LocalSettings implements Serializable {
    private boolean editorHasBeenLaunched;
    private static volatile LocalSettings instance;
    private List<Result> results = Collections.synchronizedList(new ArrayList<Result>());
    private Date lastDateEmailChecked = new Date(0L);
    private ArrayList<PlagiarismResult> plagiarismResults = new ArrayList<>();
    private ConcurrentLinkedQueue<Task> failedTasks = new ConcurrentLinkedQueue<>();
    private HashMap<String, ArrayList<Task>> subjectsAndTasks = new HashMap<>();

    private Date editedTasksDate = new Date();

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

    public Date getEditedTasksDate() {
        return editedTasksDate;
    }

    public boolean editorHasBeenLaunched() {
        return editorHasBeenLaunched;
    }

    public HashMap<String, ArrayList<Task>> getSubjectsAndTasks() {
        return subjectsAndTasks;
    }

    public void setSubjectsAndTasks(HashMap<String, ArrayList<Task>> subjectsAndTasks) {
        this.subjectsAndTasks = subjectsAndTasks;
        this.editorHasBeenLaunched = true;
    }

    public void updateTask(Task task) {
        if (subjectsAndTasks.containsKey(task.getSubjectName())) {
            if (subjectsAndTasks.get(task.getSubjectName()).contains(task))
                subjectsAndTasks.get(task.getSubjectName()).remove(task); //...
            subjectsAndTasks.get(task.getSubjectName()).add(task);
        } else {
            ArrayList<Task> tasks = new ArrayList<>();
            tasks.add(task);
            subjectsAndTasks.put(task.getSubjectName(), tasks);
        }
        editedTasksDate = new Date();
        GlobalSettings.getInstance().setEditedTasksDate(new Date());
    }

    public void deleteTask(Task task) { //TODO write this method
        editedTasksDate = new Date(); //Updating the date
        GlobalSettings.getInstance().setEditedTasksDate(new Date());
    }

    public Date getLastDateEmailChecked() {
        return this.lastDateEmailChecked;
    }

    public void setLastDateEmailChecked(Date date) {
        this.lastDateEmailChecked = date;
    }

    public ConcurrentLinkedQueue<Task> getFailedTasks() {
        return failedTasks;
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
