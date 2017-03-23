package Model;


/**
 * Created by GSatosov on 3/3/2017.
 */
public class Student {
    private String name;
    private String groupName;

    public Student(String name, String groupName) {
        this.name = name;
        this.groupName = groupName;
    }

    @Override
    public String toString() {
        return this.name + " from group " + this.groupName;
    }
}
