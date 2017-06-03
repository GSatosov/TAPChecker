package Model;


import java.io.Serializable;

/**
 * Created by GSatosov on 3/3/2017.
 */
public class Student implements Serializable {
    private String name;
    private String groupName;

    public Student(String name, String groupName) {
        this.name = name;
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return this.name + " from group " + this.groupName;
    }

    @Override
    public boolean equals(Object obj) {
        Student student = (Student) obj;
        return this.name.equals(student.getName()) && this.groupName.equals(student.getGroupName());
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() + this.groupName.hashCode() * 42;
    }
}
