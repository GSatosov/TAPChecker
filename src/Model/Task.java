package Model;

/**
 * Created by GSatosov on 3/3/2017.
 */
public class Task {
    private String subjectName;
    private String name;
    private String sourcePath;

    public String getSubjectName() {
        return subjectName;
    }

    public String getName() {
        return name;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public Task(String name, String subject, String source) {
        this.name = name;
        this.subjectName = subject;
        this.sourcePath = source;
    }

    @Override
    public String toString() {
        return "{" + this.name + ", " + this.subjectName + ", " + this.sourcePath + "}";
    }
}
