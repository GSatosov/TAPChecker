package Model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Created by GSatosov on 3/22/2017.
 */
public class Result implements Comparable, Serializable {
    private String message;
    private Task task;

    public Result(String message, Task task) {
        this.task = task;
        this.message = message;
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

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return task.getAuthor() + " has scored " + message + " on " + task.getSubjectName() + " " + task.getName() + ".";
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Result) {
            Result b = (Result) o;
            String[] errorA = getMessage().split(" ");
            String[] errorB = b.getMessage().split(" ");
            String errorACode = errorA[0], errorBCode = errorB[0];
            int errorANum = 0, errorBNum = 0;
            if (errorACode.equals("DL")) {
                errorACode += " " + errorA[1];
                if (errorA.length == 3) {
                    errorANum = Integer.parseInt(errorA[2]);
                }
            } else if (errorA.length == 2) {
                errorANum = Integer.parseInt(errorA[1]);
            }
            if (errorBCode.equals("DL")) {
                errorBCode += " " + errorB[1];
                if (errorB.length == 3) {
                    errorBNum = Integer.parseInt(errorB[2]);
                }
            } else if (errorB.length == 2) {
                errorBNum = Integer.parseInt(errorB[1]);
            }

            if (errorACode.equals(errorBCode)) {
                if (errorANum == errorBNum) {
                    return getTask().getReceivedDate() == null || b.getTask().getReceivedDate() == null ? 1 : getTask().getReceivedDate().compareTo(b.getTask().getReceivedDate());
                }
                else {
                    return Integer.compare(errorANum, errorBNum);
                }
            }
            else {
                List<String> results = Arrays.asList("CE", "RE", "TL", "WA", "DL CE", "DL RE", "DL TL", "DL WA", "DL OK", "OK");
                System.out.println(Integer.compare(results.indexOf(errorACode), results.indexOf(errorBCode)) + " " + errorACode + " " + errorBCode);
                return Integer.compare(results.indexOf(errorACode), results.indexOf(errorBCode));
            }
        }
        return 0;
    }
}
