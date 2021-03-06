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
    private String additionalTest = "";

    public Task() {

    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

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
        return "{" + this.name + ", " + this.subjectName + ", " + getStudent() + ", " + getReceivedDate() + ", " + this.sourcePath + ", " + this.timeInMS + ", " + this.antiPlagiarism + ", " + this.deadline + ", " + this.taskCode + ", " + this.hardDeadline + "}";
    }

    @Override
    public boolean equals(Object obj) {
        Task task = (Task) obj;
        int thisDot = this.getName().lastIndexOf(".");
        int objDot = task.getName().lastIndexOf(".");
        return (thisDot != -1 ? this.getName().substring(0, thisDot) : this.getName()).toLowerCase().equals((objDot != -1 ? task.getName().substring(0, objDot) : task.getName()).toLowerCase()) && this.getSubjectName().replaceAll(" ", "_").toLowerCase().equals(task.getSubjectName().replaceAll(" ", "_").toLowerCase());
    }

    @Override
    public int hashCode() {
        int thisDot = this.getName().lastIndexOf(".");
        return ((thisDot != -1 ? this.getName().substring(0, thisDot) : this.getName()).toLowerCase() + " " + this.getSubjectName().replaceAll(" ", "_").toLowerCase()).hashCode();
    }

    public String getAdditionalTest() {
        return additionalTest;
    }

    public void setAdditionalTest(String additionalTest) {
        this.additionalTest = additionalTest;
    }
}
