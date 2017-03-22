package Model;

/**
 * Created by GSatosov on 3/22/2017.
 */
public class Result {
    private String result;
    private Task task;

    public Result(String result, Task task) {
        this.task = task;
        this.result = result;
    }

    @Override
    public String toString() {
        return task.getAuthor() + " has scored " + result + " on " + task.getSubjectName() + " " + task.getName() + ".";
    }
}
