package Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by GSatosov on 3/3/2017.
 */
public class Task implements Serializable {
    private String subjectName;
    private String name;
    private String sourcePath;
    private ArrayList<Test> testContents;
    private Student author;
    private Date receivedDate;

    private String taskCode;
    private long timeInMS;
    private boolean antiPlagiarism;
    private Date deadline;
    private boolean hardDeadline;

    public void setTestFields(long timeInMS, boolean antiPlagiarism, Date deadline, String taskCode, boolean hardDeadline) {
        this.timeInMS = timeInMS;
        this.antiPlagiarism = antiPlagiarism;
        this.deadline = deadline;
        this.taskCode = taskCode;
        this.hardDeadline = hardDeadline;
    }

    public boolean hasHardDeadline() {
        return this.hardDeadline;
    }

    public long getTimeInMS() {
        return this.timeInMS;
    }

    public Date getDeadline() {
        return this.deadline;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public boolean shouldBeCheckedForAntiPlagiarism() {
        return this.antiPlagiarism;
    }

    public void setAuthor(Student author) {
        this.author = author;
    }

    Student getAuthor() {
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

    @Override
    public boolean equals(Object obj) {
        Task task = (Task) obj;
        return this.getName().equals(task.getName());
    }
}
