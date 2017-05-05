package Model;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by GSatosov on 3/3/2017.
 */
public class Task {
    private String subjectName;
    private String name;
    private String sourcePath;
    private ArrayList<Test> testContents;
    private Student author;
    private Date receivedDate;

    public void setAuthor(Student author) {
        this.author = author;
    }

    public Student getAuthor() {
        return this.author;
    }

    public Date getReceivedDate() {
        return this.receivedDate;
    }

    public ArrayList<Test> getTestContents() {
        return this.testContents;
    }

    public void setTestContents(ArrayList<Test> testContents) {
        this.testContents = testContents;
    }

    public String getSubjectName() {
        return this.subjectName;
    }

    public String getName() {
        return this.name;
    }

    public String getSourcePath() {
        return this.sourcePath;
    }

    Student getStudent() {
        return author;
    }

    String getGroup() {
        return getStudent().getGroupName();
    }

    public Task(String name, String subject, String source, Date receivedDate) {
        this.name = name;
        this.subjectName = subject;
        this.sourcePath = source;
        this.receivedDate = receivedDate;
    }

    @Override
    public String toString() {
        return "{" + this.name + ", " + this.subjectName + ", " + getStudent() + ", " + getReceivedDate() + ", " + this.sourcePath + "}";
    }
}
