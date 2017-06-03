package Model;

import java.io.Serializable;

/**
 * Created by GSatosov on 5/4/2017.
 */
public class PlagiarismResult implements Serializable {
    private String result;
    private Task taskFromFirstStudent;
    private Task taskFromSecondStudent;

    public String getResult() {
        return result;
    }

    public Task getTaskFromFirstStudent() {
        return taskFromFirstStudent;
    }


    public Task getTaskFromSecondStudent() {
        return taskFromSecondStudent;
    }

    public PlagiarismResult(String result, Task task1, Task task2) {
        this.result = result;
        if (task1.getReceivedDate().getTime() < task2.getReceivedDate().getTime()) {
            this.taskFromFirstStudent = task1;
            this.taskFromSecondStudent = task2;
        } else {
            this.taskFromFirstStudent = task2;
            this.taskFromSecondStudent = task1;
        }
    }

    public String getFirstStudentName() {
        return getTaskFromFirstStudent().getStudent().getName();
    }

    public String getSecondStudentName() {
        return getTaskFromSecondStudent().getStudent().getName();
    }

    public String getFirstStudentGroupName() {
        return getTaskFromFirstStudent().getStudent().getGroupName();
    }

    public String getSecondStudentGroupName() {
        return getTaskFromSecondStudent().getStudent().getGroupName();
    }

    public String getSubject() {
        return getTaskFromFirstStudent().getSubjectName();
    }

    public String getTaskName() {
        return getTaskFromFirstStudent().getName();
    }

    public String getTaskCode() {
        return getTaskFromFirstStudent().getTaskCode();
    }

    public PlagiarismResult(Task task1, Task task2) {
        if (task1.getReceivedDate().getTime() < task2.getReceivedDate().getTime()) {
            this.taskFromFirstStudent = task1;
            this.taskFromSecondStudent = task2;
        } else {
            this.taskFromFirstStudent = task2;
            this.taskFromSecondStudent = task1;
        }
    }

    @Override
    public String toString() {
        return "The probability of plagiarism between " + taskFromFirstStudent.getAuthor() + " and " + taskFromSecondStudent.getAuthor()
                + " on task " + taskFromFirstStudent.getSubjectName() + " " + taskFromFirstStudent.getName() + " is " + result;
    }

    @Override
    public boolean equals(Object obj) {
        PlagiarismResult result = (PlagiarismResult) obj;
        return this.taskFromFirstStudent.getName().equals(result.taskFromFirstStudent.getName()) &&
                this.taskFromFirstStudent.getAuthor().equals(result.taskFromFirstStudent.getAuthor()) &&
                this.taskFromSecondStudent.getAuthor().equals(result.taskFromSecondStudent.getAuthor());
    }

    @Override
    public int hashCode() {
        return this.taskFromFirstStudent.getName().hashCode() + (this.taskFromFirstStudent.getAuthor().hashCode() + this.taskFromSecondStudent.getAuthor().hashCode()) * 42;
    }
}
