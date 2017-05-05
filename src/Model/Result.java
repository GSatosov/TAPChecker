package Model;

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
            else if (!getMessage().equals("CE") && b.getMessage().equals("CE")) return 1;
            else if (getMessage().equals("CE") && !b.getMessage().equals("CE")) return -1;
            else {
                String[] errorA = getMessage().split(" ");
                String[] errorB = getMessage().split(" ");
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
