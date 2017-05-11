package Model;

/**
 * Created by GSatosov on 5/4/2017.
 */
public class PlagiarismResult {
    private String result;
    private Task taskFromFirstStudent;
    private Task taskFromSecondStudent;

    public PlagiarismResult(String result, Task task1, Task task2) {
        this.result = result;
        this.taskFromFirstStudent = task1;
        this.taskFromSecondStudent = task2;
    }

    @Override
    public String toString() {
        return "The probability of plagiarism between " + taskFromFirstStudent.getAuthor() + " and " + taskFromSecondStudent.getAuthor()
                + " on task " + taskFromFirstStudent.getSubjectName() + " " + taskFromFirstStudent.getName() + " is " + result;
    }
}
