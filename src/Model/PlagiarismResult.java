package Model;

import java.io.Serializable;

/**
 * Created by GSatosov on 5/4/2017.
 */
public class PlagiarismResult implements Serializable {
    private String result;
    private Task taskFromFirstStudent;
    private Task taskFromSecondStudent;
    public Task getTaskFromFirstStudent() {
        return taskFromFirstStudent;
    }



    public Task getTaskFromSecondStudent() {
        return taskFromSecondStudent;
    }



    public PlagiarismResult(String result, Task task1, Task task2) {
        this.result = result;
        this.taskFromFirstStudent = task1;
        this.taskFromSecondStudent = task2;
    }

    public PlagiarismResult(Task task1, Task task2) {
        this.taskFromFirstStudent = task1;
        this.taskFromSecondStudent = task2;
    }

    @Override
    public String toString() {
        return "The probability of plagiarism between " + taskFromFirstStudent.getAuthor() + " and " + taskFromSecondStudent.getAuthor()
                + " on task " + taskFromFirstStudent.getSubjectName() + " " + taskFromFirstStudent.getName() + " is " + result;
    }

    @Override
    public boolean equals(Object obj) {
        PlagiarismResult result = (PlagiarismResult) obj;
        return this.taskFromFirstStudent.toString().equals(result.taskFromFirstStudent.toString()) &&
                this.taskFromSecondStudent.toString().equals(result.taskFromSecondStudent.toString()) &&
                this.taskFromSecondStudent.getName().equals(result.taskFromSecondStudent.getName());
    }
}
