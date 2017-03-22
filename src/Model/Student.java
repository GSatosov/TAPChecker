package Model;


/**
 * Created by GSatosov on 3/3/2017.
 */
class Student {
    private String name;
    private String groupName;

    @Override
    public String toString() {
        return this.name + " from group " + this.groupName;
    }
}
