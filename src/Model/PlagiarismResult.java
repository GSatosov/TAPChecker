package Model;

/**
 * Created by GSatosov on 5/4/2017.
 */
public class PlagiarismResult {
    private String result;
    private Student firstStudent;
    private Student secondStudent;
    private Task task;

    public PlagiarismResult(String result, Student firstStudent, Student secondStudent, Task task) {
        this.result = result;
        this.firstStudent = firstStudent;
        this.secondStudent = secondStudent;
        this.task = task;
    }

    @Override
    public String toString() {
        return "The probability of plagiarism between " + firstStudent + " and " + secondStudent + " on task " + task.getSubjectName() + " " + task.getName() + " is " + result;
    }
}
