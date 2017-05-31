package View;

import Model.PlagiarismResult;
import Model.Student;
import Model.Task;
import javafx.geometry.HPos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;


import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
        return mainPane;
    }

    private GridPane constructTable(String subject, String task) {
        GridPane table = new GridPane();

        ArrayList<Student> students = new ArrayList<>();
        ArrayList<PlagiarismResult> results = data.stream().filter(result -> result.getTaskFromFirstStudent().getName().equals(task) && result.getTaskFromFirstStudent().getSubjectName().equals(subject)).collect(Collectors.toCollection(ArrayList::new));
        results.forEach(result -> {
            if (!students.contains(result.getTaskFromFirstStudent().getAuthor()))
                students.add(result.getTaskFromFirstStudent().getAuthor());
            if (!students.contains(result.getTaskFromSecondStudent().getAuthor()))
                students.add(result.getTaskFromSecondStudent().getAuthor());
        });
        for (int i = 0; i < students.size(); i++) { //Adding column and row names
            Text columnResult = new Text(Arrays.stream(students.get(i).getName().split(" ")).reduce("", (a, b) -> a.concat("\n").concat(b)) + ",\n" + students.get(i).getGroupName());
            Text rowResult = new Text(Arrays.stream(students.get(i).getName().split(" ")).reduce("", (a, b) -> a.concat("\n").concat(b)) + ",\n" + students.get(i).getGroupName());
            table.add(columnResult, 0, i + 1);
            table.add(rowResult, i + 1, 0);
            GridPane.setHalignment(rowResult, HPos.CENTER);
        }
        for (int i = 1; i <= students.size(); i++)
            for (int j = 1; j <= students.size(); j++) {
                if (i == j) {
                    Text emptyCell = new Text("X");
                    table.add(emptyCell, i, j);
                    GridPane.setHalignment(emptyCell, HPos.CENTER);
                } else {
                    PlagiarismResult result = findResult(students.get(j - 1), students.get(i - 1), results);
                    Button resultButton = new Button(result.getResult());
                    resultButton.setStyle("-fx-background-color: transparent");
                    ContextMenu menu = new ContextMenu();
                    MenuItem item = new MenuItem("Open " + result.getTaskFromFirstStudent().getAuthor().getName() + "'s code.");
                    item.setOnAction(event1 -> {
                        if (Desktop.isDesktopSupported())
                            try {
                                Desktop.getDesktop().open(new File(result.getTaskFromFirstStudent().getSourcePath()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    });
                    MenuItem item2 = new MenuItem("Open " + result.getTaskFromSecondStudent().getAuthor().getName() + "'s code.");
                    item2.setOnAction(event1 -> {
                        if (Desktop.isDesktopSupported())
                            try {
                                Desktop.getDesktop().open(new File(result.getTaskFromSecondStudent().getSourcePath()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    });
                    menu.getItems().add(item);
                    menu.getItems().add(item2);
                    resultButton.setContextMenu(menu);
                    table.add(resultButton, i, j);
                }
            }
        return table;
    }

    private PlagiarismResult findResult(Student student1, Student student2, ArrayList<PlagiarismResult> results) {
        return results.stream().filter(result -> ((result.getTaskFromFirstStudent().getAuthor().equals(student1) && result.getTaskFromSecondStudent().getAuthor().equals(student2))
                || (result.getTaskFromFirstStudent().getAuthor().equals(student2) && result.getTaskFromSecondStudent().getAuthor().equals(student1)))).findFirst().get();
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
        subjectsBox.getSelectionModel().selectFirst();
        tasksBox.getItems().addAll(subjectsAndTasks.get(subjectsBox.getValue()));
        tasksBox.getSelectionModel().selectFirst();
        mainPane.setCenter(constructTable(subjectsBox.getValue(), tasksBox.getValue()));
        pane.add(subjectsBox, 0, 1);
        pane.add(tasksBox, 1, 1);
        return pane;
    }
}
