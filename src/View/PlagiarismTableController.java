package View;

import Controller.GoogleDriveManager;
import Model.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.util.*;


import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Created by GSatosov on 5/31/2017.
 */
public class PlagiarismTableController implements Initializable {

    @FXML
    private TableView plagiarismTable;

    @FXML
    private ChoiceBox subjectsList;

    @FXML
    private ChoiceBox tasksList;

    private class PlagiarismResultsTableCellObject {

        private String item;

        private PlagiarismResult plagiarismResult;

        public PlagiarismResultsTableCellObject(String item) {
            this.item = item;
        }

        public PlagiarismResultsTableCellObject(String item, PlagiarismResult plagiarismResult) {
            this.item = item;
            this.plagiarismResult = plagiarismResult;
        }

        public String getItem() {
            return this.item;
        }

        public PlagiarismResult getPlagiarismResult() {
            return this.plagiarismResult;
        }

        @Override
        public String toString() {
            return this.getItem();
        }
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        ArrayList<PlagiarismResult> plagiarismResults = LocalSettings.getInstance().getPlagiarismResults();
        Map<String, List<PlagiarismResult>> resultsBySubject = plagiarismResults.stream().collect(Collectors.groupingBy(PlagiarismResult::getSubject));

        subjectsList.getItems().clear();
        subjectsList.getItems().addAll(resultsBySubject.keySet().stream().map(s -> s.replaceAll("_", " ")).collect(Collectors.toSet()));
        subjectsList.setValue(subjectsList.getItems().get(0));

        subjectsList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            tasksList.getItems().clear();
            tasksList.getItems().addAll(resultsBySubject.get(newValue.toString().replaceAll(" ", "_")).stream().map(result -> result.getTaskCode() + ": " + result.getTaskName()).collect(Collectors.toSet()).stream().sorted(String::compareTo).collect(Collectors.toList()));
            tasksList.setValue(tasksList.getItems().get(0));
        });

        tasksList.getItems().clear();
        tasksList.getItems().addAll(resultsBySubject.get(subjectsList.getValue().toString().replaceAll("_", " ")).stream().map(result -> result.getTaskCode() + ": " + result.getTaskName()).collect(Collectors.toSet()).stream().sorted(String::compareTo).collect(Collectors.toList()));
        tasksList.setValue(tasksList.getItems().get(0));

        tasksList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null)
            fillTable(resultsBySubject.get(subjectsList.getValue().toString().replace(" ", "_")).stream()
                .filter(result -> result.getTaskName().equals(newValue.toString().split(": ")[1])).collect(Collectors.toList()));

        });

        fillTable(resultsBySubject.get(subjectsList.getValue().toString().replace(" ", "_")).stream()
                .filter(result -> result.getTaskName().equals(tasksList.getValue().toString().split(": ")[1])).collect(Collectors.toList()));

    }

    private void fillTable(List<PlagiarismResult> results) {

        plagiarismTable.getItems().clear();
        plagiarismTable.getColumns().clear();

        ArrayList<String> students = new ArrayList<>();
        students.addAll(results.stream().map(result -> result.getFirstStudentName() + " (" + result.getFirstStudentGroupName() + ")").collect(Collectors.toSet()));
        students.addAll(results.stream().map(result -> result.getSecondStudentName() + " (" + result.getSecondStudentGroupName() + ")").collect(Collectors.toSet()));
        students = new ArrayList<>((new HashSet<>(students)));
        students.sort(String::compareTo);

        plagiarismTable.getColumns().add(getColumn("Full Name"));

        students.forEach(student -> {
            plagiarismTable.getColumns().add(getColumn(student));
        });

        HashMap<String, HashMap<String, PlagiarismResultsTableCellObject>> rows = new HashMap<>();

        students.forEach(student -> {
            HashMap<String, PlagiarismResultsTableCellObject> studentHM = new HashMap<>();
            studentHM.put("Full Name", new PlagiarismResultsTableCellObject(student));
            results.stream().filter(result -> (result.getFirstStudentName() + " (" + result.getFirstStudentGroupName() + ")").equals(student) || (result.getSecondStudentName() + " (" + result.getSecondStudentGroupName() + ")").equals(student)).forEach(studentResult -> {
                String secondStudent = (studentResult.getFirstStudentName() + " (" + studentResult.getFirstStudentGroupName() + ")").equals(student) ? studentResult.getSecondStudentName() + " (" + studentResult.getSecondStudentGroupName() + ")" : studentResult.getFirstStudentName() + " (" + studentResult.getFirstStudentGroupName() + ")";
                studentHM.put(secondStudent, new PlagiarismResultsTableCellObject(studentResult.getResult(), studentResult));
            });

            rows.put(student, studentHM);
        });

        rows.keySet().stream().sorted(String::compareTo).forEach(student -> {
            plagiarismTable.getItems().add(rows.get(student));
        });
    }

    private static TableColumn<Map, PlagiarismResultsTableCellObject> getColumn(String columnTitle) {
        TableColumn<Map, PlagiarismResultsTableCellObject> column = new TableColumn<>(columnTitle);
        column.setCellValueFactory(new MapValueFactory(columnTitle));
        column.setSortable(false);
        column.setCellFactory(new javafx.util.Callback<TableColumn<Map, PlagiarismResultsTableCellObject>, TableCell<Map, PlagiarismResultsTableCellObject>>() {
            @Override
            public TableCell call(TableColumn p) {
                TableCell cell = new TableCell<String, PlagiarismResultsTableCellObject>() {
                    @Override
                    public void updateItem(final PlagiarismResultsTableCellObject item, final boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item.getItem());
                            int lowerBoundary = 40;

                            if (item.getPlagiarismResult() == null) {
                                setStyle("-fx-font-weight:bold;");
                            }
                            else {
                                double doubleResult = Double.parseDouble(item.getPlagiarismResult().getResult().replaceAll("%", "").replaceAll(",", "."));
                                if (doubleResult >= lowerBoundary){
                                    setStyle("-fx-background-color: rgb(255, " + (Math.max(0, Math.min(Math.round((100 - doubleResult) / (100 - lowerBoundary) * 255), 255)))  + ", 0);");
                                }
                                else {
                                    setStyle("");
                                }

                                final ContextMenu contextMenu = new ContextMenu();
                                MenuItem codeFirst = new MenuItem("Open " + item.getPlagiarismResult().getFirstStudentName() + "'s code");
                                MenuItem codeSecond = new MenuItem("Open " + item.getPlagiarismResult().getSecondStudentName() + "'s code");
                                contextMenu.getItems().addAll(codeFirst, codeSecond);
                                codeFirst.setOnAction(event -> {
                                    if (Desktop.isDesktopSupported())
                                        try {
                                            Desktop.getDesktop().open(new File(item.getPlagiarismResult().getTaskFromFirstStudent().getSourcePath()));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                });
                                codeSecond.setOnAction(event -> {
                                    if (Desktop.isDesktopSupported())
                                        try {
                                            Desktop.getDesktop().open(new File(item.getPlagiarismResult().getTaskFromSecondStudent().getSourcePath()));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                });
                                this.setContextMenu(contextMenu);
                            }
                        } else {
                            setText(null);
                            setStyle("");
                        }
                    }
                };

                return cell;
            }
        });
        return column;
    }

}
