package View;

import Controller.General;
import Controller.GoogleDriveManager;
import Model.GlobalSettings;
import Model.LocalSettings;
import Model.Result;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Arseniy Nazarov on 20.04.17.
 */
public class MainController implements Initializable {

    @FXML
    private TabPane resultsTable;

    @FXML
    private BorderPane plagiarism;

    @FXML
    private Button runTests;
    @FXML
    private Button plagiarismResults;
    @FXML
    private Button settings;
    @FXML
    private Button editTasks;
    private static Stage settingsFrame;
    private int plagiarismListSize = -1; //H A C K S
    private Stage plagiarismStage;

    static Stage getSettingsFrame() {
        return settingsFrame;
    }

    private class ResultsTableCellObject {

        private String item;

        private Result result;

        public ResultsTableCellObject(String item) {
            this.item = item;
        }

        public ResultsTableCellObject(String item, Result result) {
            this.item = item;
            this.result = result;
        }

        public String getItem() {
            return this.item;
        }

        public Result getResult() {
            return this.result;
        }

        @Override
        public String toString() {
            return this.getItem();
        }
    }

    public void showResults() {
        resultsTable.getTabs().clear();
        try {
            HashMap<String, ArrayList<String>> taskNumbersForSubjects = GoogleDriveManager.getTaskNumbersForSubjects();
            // group by subject
            LocalSettings.getInstance().getResults().stream().collect(Collectors.groupingBy(Result::getSubject)).forEach((String subject, List<Result> subjectResults) -> {
                ArrayList<String> subjectTaskNumbers = taskNumbersForSubjects.get(subject);
                TableView currentTable = new TableView();

                currentTable.setColumnResizePolicy(p -> true);

                currentTable.getColumns().add(getColumn("ФИО"));;

                for (String subjectTaskNumber : subjectTaskNumbers) {
                    currentTable.getColumns().add(getColumn(subjectTaskNumber));
                }

                // group by group
                subjectResults.stream().collect(Collectors.groupingBy(Result::getGroup)).forEach((group, groupResults) -> {
                    HashMap<String, ResultsTableCellObject> groupHM = new HashMap<>();
                    groupHM.put("ФИО", new ResultsTableCellObject("Группа " + group));
                    currentTable.getItems().add(groupHM);

                    Set<String> studentsNames = groupResults.stream().map(r -> r.getStudent().getName()).collect(Collectors.toSet());
                    studentsNames.stream().sorted(String::compareTo).forEach(student -> {
                        HashMap<String, ResultsTableCellObject> studentHM = new HashMap<>();
                        studentHM.put("ФИО", new ResultsTableCellObject(student));
                        groupResults.stream().filter(result -> result.getStudent().getName().equals(student)).forEach(studentResult -> {
                            studentHM.put(studentResult.getTask().getTaskCode(), new ResultsTableCellObject(studentResult.getMessage(), studentResult));
                        });
                        currentTable.getItems().add(studentHM);
                    });

                    HashMap<String, ResultsTableCellObject> emptyHM = new HashMap<>();
                    emptyHM.put("ФИО", new ResultsTableCellObject(""));
                    currentTable.getItems().add(emptyHM);
                });

                Tab tab = new Tab(subject, currentTable);
                tab.setClosable(false);
                resultsTable.getTabs().add(tab);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static TableColumn<Map, ResultsTableCellObject> getColumn(String columnTitle) {
        TableColumn<Map, ResultsTableCellObject> column = new TableColumn<>(columnTitle);
        column.setCellValueFactory(new MapValueFactory(columnTitle));
        column.setCellFactory(new Callback<TableColumn<Map, ResultsTableCellObject>, TableCell<Map, ResultsTableCellObject>>() {
            @Override
            public TableCell call(TableColumn p) {
                TableCell cell = new TableCell<String, ResultsTableCellObject>() {
                    @Override
                    public void updateItem(final ResultsTableCellObject item, final boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item.getItem());
                            if (item.getItem().startsWith("Группа ")) {
                                setStyle("-fx-font-weight:bold;");
                            }
                            else if (item.getResult() != null && item.getResult().getTask().getReceivedDate().compareTo(item.getResult().getTask().getDeadline()) > 0) {
                                setStyle("-fx-background-color: " + (item.getResult().getTask().hasHardDeadline() ? "red" : "yellow") + ";");
                            }
                            else setStyle("");

                            if (item.getResult() != null) {
                                final ContextMenu contextMenu = new ContextMenu();
                                MenuItem code = new MenuItem("Open " + item.getResult().getStudent().getName() + "'s code.");
                                MenuItem log = new MenuItem("Open " + item.getResult().getStudent().getName() + "'s log.");
                                contextMenu.getItems().addAll(code, log);
                                code.setOnAction(event -> {
                                    if (Desktop.isDesktopSupported())
                                        try {
                                            Desktop.getDesktop().open(new File(item.getResult().getTask().getSourcePath()));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                });
                                log.setOnAction(event -> {
                                    if (Desktop.isDesktopSupported())
                                        try {
                                            String sourcePath = item.getResult().getTask().getSourcePath();
                                            Desktop.getDesktop().open(new File(sourcePath.substring(0, sourcePath.lastIndexOf('.')) + "Output.txt"));
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new File(GlobalSettings.getDataFolder()).mkdirs();
        runTests.setOnAction(event -> {
            runTests.setDisable(true);
            try {
                General.getResults(() -> runTests.setDisable(false), this);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        plagiarismResults.setOnAction(event -> {
            if (LocalSettings.getInstance().getPlagiarismResults().size() > 0) {
                if (LocalSettings.getInstance().getPlagiarismResults().size() > plagiarismListSize) {
                    plagiarism = new PlagiarismTableController(LocalSettings.getInstance().getPlagiarismResults()).getPane();
                    plagiarismListSize = LocalSettings.getInstance().getPlagiarismResults().size();
                    plagiarismStage = new Stage();
                    plagiarismStage.setScene(new Scene(plagiarism, 500, 480));
                    plagiarismStage.show();
                } else plagiarismStage.show();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "No plagiarism results have been acquired. Run tests to get them.");
                alert.showAndWait();
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
            settingsFrame.setTitle("GlobalSettings");
            settingsFrame.setResizable(false);
            settingsFrame.show();
        });
        editTasks.setOnAction(event -> new TaskEditorController().getStage().show());
    }
}
