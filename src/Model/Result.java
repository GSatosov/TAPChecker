package Model;

import com.sun.istack.internal.NotNull;

/**
 * Created by GSatosov on 3/22/2017.
 */
public class Result implements Comparable {
    private String result;
    private Task task;

    public Result(String result, Task task) {
        this.task = task;
        this.result = result;
    }

    public Student getStudent() {
        return task.getStudent();
    }

    public String getGroup() {
        return task.getGroup();
    }

    public String getSubject() {
        return task.getSubjectName().replaceAll("_", " ");
    }

    public Task getTask() {
        return task;
    }

    public String getResult() {
        return result;
    }

    @Override
    public String toString() {
        return task.getAuthor() + " has scored " + result + " on " + task.getSubjectName() + " " + task.getName() + ".";
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Result) {
            Result b = (Result) o;
            if (getResult().equals(b.getResult())) {
                return getTask().getReceivedDate() == null || b.getTask().getReceivedDate() == null ? 1 : getTask().getReceivedDate().compareTo(b.getTask().getReceivedDate());
            } else if (getResult().equals("OK") && !b.getResult().equals("OK")) return 1;
            else if (!getResult().equals("OK") && b.getResult().equals("OK")) return -1;
            else if (!getResult().equals("CE") && b.getResult().equals("CE")) return 1;
            else if (getResult().equals("CE") && !b.getResult().equals("CE")) return -1;
            else {
                String[] errorA = getResult().split(" ");
                String[] errorB = getResult().split(" ");
                if (errorA[0].equals(errorB[0])) {
                    Integer aR = Integer.parseInt(errorA[1]);
                    Integer bR = Integer.parseInt(errorB[1]);
                    return aR.compareTo(bR);
                } else {
                    return (errorA[0].equals("RE") || errorB[0].equals("WA")) ? -1 : 1;
                }
            }
        }
        return 0;
    }
}
