package Controller;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by GSatosov on 3/3/2017.
 */
class Subject {
    private String Name;

    private ArrayList<Group> Groups;
    public String getName() {
        return Name;
    }

    public ArrayList<Group> getGroups() {
        return Groups;
    }

    Subject(String Name, Group group) {
        this.Name = Name;
        this.Groups.add(group);
        new File(this.Name).mkdir();
    }

}
