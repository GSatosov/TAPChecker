package Model;

import java.util.ArrayList;
/**
 * Created by GSatosov on 3/3/2017.
 */
public class Task {
    private String subjectName;
    private String name;
    private String sourcePath;
    private ArrayList<Test> testContents;

    public ArrayList<Test> getTestContents() {
        return testContents;
    }

    public void setTestContents(ArrayList<Test> testContents) {
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

    @Override
    public String toString() {
        return "{" + this.name + ", " + this.subjectName + ", " + this.sourcePath + "}";
    }
}
