package Model;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by GSatosov on 3/3/2017.
 */
public class Subject {
    private String name;

    private ArrayList<Group> Groups;

    public String getName() {
        return name;
    }

    public ArrayList<Group> getGroups() {
        return Groups;
    }

    public Subject(String name, Group group) {
        this.name = name;
        this.Groups.add(group);
        new File(this.name).mkdir();
    }

    public Subject(String name) {
        this.name = name;
    }
}
