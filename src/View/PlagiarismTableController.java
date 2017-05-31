package View;

import Model.PlagiarismResult;
import Model.Task;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Created by GSatosov on 5/31/2017.
 */
class PlagiarismTableController {
    private ArrayList<PlagiarismResult> data;

    PlagiarismTableController(ArrayList<PlagiarismResult> results) {
        this.data = results;
    }

    BorderPane getPane() {
        BorderPane mainPane = new BorderPane();
        mainPane.setTop(dropdownListsPane(mainPane));
        TabPane pane = new TabPane();
        mainPane.setCenter(pane);
        return mainPane;
    }

    private TableView constructTable(String subject, String task) {
        TableView table = new TableView();
        ArrayList<PlagiarismResult> results = data.stream().filter(result -> result.getTaskFromFirstStudent().getName().equals(task) && result.getTaskFromFirstStudent().getSubjectName().equals(subject)).collect(Collectors.toCollection(ArrayList::new));
        TableColumn verticalNames = new TableColumn();
        TableRow horizontalNames = new TableRow();
        return table;
    }

    private HashMap<String, ArrayList<String>> getSubjectsAndTasks() {
        HashMap<String, ArrayList<String>> subjectsAndTasks = new HashMap<>();
        data.forEach(result -> {
            Task task = result.getTaskFromFirstStudent();
            if (subjectsAndTasks.containsKey(task.getSubjectName())) {
                if (!subjectsAndTasks.get(task.getSubjectName()).contains(task.getName()))
                    subjectsAndTasks.get(task.getSubjectName()).add(task.getName());
            } else {
                ArrayList<String> taskNames = new ArrayList<>();
                taskNames.add(task.getName());
                subjectsAndTasks.put(task.getSubjectName(), taskNames);
            }
        });
        return subjectsAndTasks;
    }

    private GridPane dropdownListsPane(BorderPane mainPane) {
        GridPane pane = new GridPane();
        HashMap<String, ArrayList<String>> subjectsAndTasks = getSubjectsAndTasks();
        ComboBox<String> subjectsBox = new ComboBox<>();
        ComboBox<String> tasksBox = new ComboBox<>();
        subjectsBox.getItems().addAll(subjectsAndTasks.keySet());
        subjectsBox.setOnAction(event -> {
            tasksBox.getSelectionModel().clearSelection();
            tasksBox.getItems().clear();
            tasksBox.getItems().addAll(subjectsAndTasks.get(subjectsBox.getValue()));
        });
        tasksBox.setOnAction(event -> mainPane.setCenter(constructTable(subjectsBox.getValue(), tasksBox.getValue())));
        pane.add(subjectsBox, 0, 1);
        pane.add(tasksBox, 1, 1);
        return pane;
    }
}
