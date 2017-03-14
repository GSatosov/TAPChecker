package Model;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by GSatosov on 3/3/2017.
 */
public class Task {
    private String subjectName;
    private String name;
    private String sourcePath;
    private HashMap<String, ArrayList<String>> testContents;

    public HashMap<String, ArrayList<String>> getTestContents() {
        return testContents;
    }


    public void setTestContents(HashMap<String, ArrayList<String>> testContents) {
        this.testContents = testContents;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getName() {
        return name;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public Task(String name, String subject, String source) {
        this.name = name;
        this.subjectName = subject;
        this.sourcePath = source;
    }
}
