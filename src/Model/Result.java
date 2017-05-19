package Model;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by GSatosov on 3/22/2017.
 */
public class Result implements Comparable {
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
            if (getMessage().equals(b.getMessage())) {
                return getTask().getReceivedDate() == null || b.getTask().getReceivedDate() == null ? 1 : getTask().getReceivedDate().compareTo(b.getTask().getReceivedDate());
            } else if (getMessage().equals("OK") && !b.getMessage().equals("OK")) return 1;
            else if (!getMessage().equals("OK") && b.getMessage().equals("OK")) return -1;
            else if (!getMessage().equals("DL") && b.getMessage().equals("DL")) return 1;
            else if (getMessage().equals("DL") && !b.getMessage().equals("DL")) return -1;
            else {
                String[] errorA = getMessage().split(" ");
                String[] errorB = getMessage().split(" ");
                if (errorA.length != errorB.length) return Integer.compare(errorA.length, errorB.length);
                else if (errorA[Math.max(errorA.length - 2, 0)].equals(errorB[Math.max(errorB.length - 2, 0)]) && errorA.length > 1 && errorB.length > 1) {
                    return Integer.compare(Integer.parseInt(errorA[errorA.length - 1]), Integer.parseInt(errorB[errorB.length - 1]));
                }
                else {
                    List<String> results = Arrays.asList(new String[] {"CE", "RE", "TL", "WA"});
                    return Integer.compare(results.indexOf(errorA[Math.max(errorA.length - 2, 0)]), results.indexOf(errorB[Math.max(errorB.length - 2, 0)]));
                }
            }
        }
        return 0;
    }
}
