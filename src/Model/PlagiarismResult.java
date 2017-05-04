package Model;

/**
 * Created by GSatosov on 5/4/2017.
 */
public class PlagiarismResult {
    private String result;
    private Student firstStudent;
    private Student secondStudent;
    private String taskName;

    public PlagiarismResult(String result, Student firstStudent, Student secondStudent, String taskName) {
        this.result = result;
        this.firstStudent = firstStudent;
        this.secondStudent = secondStudent;
        this.taskName = taskName;
    }

    @Override
    public String toString() {
        return "The probability of plagiarism between " + firstStudent + " and " + secondStudent + " on task " + taskName + " is " + result;
    }
}
