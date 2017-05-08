package View;

import Controller.General;
import Model.Result;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Arseniy Nazarov on 20.04.17.
 */
public class MainController implements Initializable {

    @FXML
    private TabPane resultsTable;

    @FXML
    private TabPane plagiary;

    @FXML
    private Button tests;
    @FXML
    private Button switchTables;
    @FXML
    private Button settings;

    private static Stage settingsFrame;

    static Stage getSettingsFrame(){
        return settingsFrame;
    }

    public void showResults(ArrayList<Result> results) {
        resultsTable.getTabs().clear();
        HashMap<String, ArrayList<Result>> resultsSplitSubjects = new HashMap<>();
        results.stream().forEach(r -> {
            if (!resultsSplitSubjects.containsKey(r.getSubject())) {
                resultsSplitSubjects.put(r.getSubject(), new ArrayList<>());
            }
            resultsSplitSubjects.get(r.getSubject()).add(r);
        });
        resultsSplitSubjects.forEach((k, v) -> {
            TableView currentTable = new TableView();
            addColumn(currentTable, "ФИО");
            for (int i = 1; i < 100; i++) {
                addColumn(currentTable, i + "");
            }

            HashMap<String, ArrayList<Result>> groupResults = new HashMap<>();
            v.forEach(r -> {
                if (!groupResults.containsKey(r.getGroup())) {
                    groupResults.put(r.getGroup(), new ArrayList<>());
                }
                groupResults.get(r.getGroup()).add(r);
            });
            groupResults.forEach((kGroup, vGroup) -> {
                HashMap<String, String> groupHM = new HashMap<>();
                groupHM.put("ФИО", "Группа " + kGroup);
                ArrayList<String> tasks = new ArrayList((vGroup.stream().map(r -> r.getTask().getName()).collect(Collectors.toSet())));
                tasks.sort(String::compareTo);
                for (int i = 0; i < tasks.size(); i++) {
                    groupHM.put((i + 1) + "", tasks.get(i));
                }
                currentTable.getItems().add(groupHM);
                HashMap<String, ArrayList<String>> studentsResults = new HashMap<>();
                vGroup.forEach(gR -> {
                    ArrayList<String> resultsStudent = studentsResults.get(gR.getStudent().getName());
                    if (!studentsResults.containsKey(gR.getStudent().getName())) {
                        studentsResults.put(gR.getStudent().getName(), new ArrayList<String>());
                        resultsStudent = studentsResults.get(gR.getStudent().getName());
                        resultsStudent.add(gR.getStudent().getName());
                        for (int i = 0; i < tasks.size(); i++) resultsStudent.add("");
                    }
                    resultsStudent.set(tasks.indexOf(gR.getTask().getName()) + 1, gR.getMessage());
                });
                studentsResults.forEach((kStudent, vStudent) -> {
                    HashMap<String, String> studentHM = new HashMap<>();
                    studentHM.put("ФИО", vStudent.get(0));
                    for (int i = 1; i < vStudent.size(); i++) {
                        studentHM.put(i + "", vStudent.get(i));
                    }
                    currentTable.getItems().add(studentHM);
                });
                HashMap<String, String> emptyHM = new HashMap<>();
                emptyHM.put("ФИО", "");
                currentTable.getItems().add(emptyHM);
            });
            Tab tab = new Tab(k, currentTable);
            tab.setClosable(false);
            resultsTable.getTabs().add(tab);
        });
    }

    private static void addColumn(TableView table, String columnTitle) {
        TableColumn<Map, String> column = new TableColumn<>(columnTitle);
        column.setCellValueFactory(new MapValueFactory(columnTitle));
        column.setMinWidth(130);
        table.getColumns().add(column);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tests.setOnAction(event -> {
            tests.setDisable(true);
            General.getResults(() -> tests.setDisable(false), this);
        });

        switchTables.setOnAction(event -> {
            if (resultsTable.isVisible()) {
                resultsTable.setVisible(false);
                plagiary.setVisible(true);
            } else {
                resultsTable.setVisible(true);
                plagiary.setVisible(false);
            }
        });

        settings.setOnAction(event -> {
            settingsFrame = new Stage();
            try {
                settingsFrame.setScene(new Scene(new FXMLLoader(getClass().getResource("Settings.fxml")).load(), 600, 400));
            } catch (IOException e) {
                e.printStackTrace();
            }

            settingsFrame.initModality(Modality.WINDOW_MODAL);
            settingsFrame.initOwner(MainFrame.getPrimaryStage());
            settingsFrame.setTitle("Settings");
            settingsFrame.setResizable(false);
            settingsFrame.show();
        });
    }
}
