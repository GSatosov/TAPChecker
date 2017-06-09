package View;

import Controller.General;
import Controller.GoogleDriveManager;
import Model.EmailHandlerData;
import Model.GlobalSettings;
import Model.LocalSettings;
import Model.Result;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import sun.applet.Main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Created by Arseniy Nazarov on 20.04.17.
 */
public class MainController implements Initializable {

    @FXML
    public Button runUncheckedTasks;

    @FXML
    private TabPane resultsTable;
    @FXML
    private Button runLocalTasks;
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
    @FXML
    private Button runFailedTasks;
    private static Stage settingsFrame;
    private Stage plagiarismStage;

    private static Stage emailHandler;

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

                currentTable.getColumns().add(getColumn("Full Name"));

                for (String subjectTaskNumber : subjectTaskNumbers) {
                    currentTable.getColumns().add(getColumn(subjectTaskNumber));
                }

                // group by group
                subjectResults.stream().collect(Collectors.groupingBy(Result::getGroup)).forEach((group, groupResults) -> {
                    HashMap<String, ResultsTableCellObject> groupHM = new HashMap<>();
                    groupHM.put("Full Name", new ResultsTableCellObject("Group " + group));
                    currentTable.getItems().add(groupHM);

                    Set<String> studentsNames = groupResults.stream().map(r -> r.getStudent().getName()).collect(Collectors.toSet());
                    studentsNames.stream().sorted(String::compareTo).forEach(student -> {
                        HashMap<String, ResultsTableCellObject> studentHM = new HashMap<>();
                        studentHM.put("Full Name", new ResultsTableCellObject(student));
                        groupResults.stream().filter(result -> result.getStudent().getName().equals(student)).forEach(studentResult -> {
                            studentHM.put(studentResult.getTask().getTaskCode(), new ResultsTableCellObject(studentResult.getMessage(), studentResult));
                        });
                        currentTable.getItems().add(studentHM);
                    });

                    HashMap<String, ResultsTableCellObject> emptyHM = new HashMap<>();
                    emptyHM.put("Full Name", new ResultsTableCellObject(""));
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

    private TableColumn<Map, ResultsTableCellObject> getColumn(String columnTitle) {
        TableColumn<Map, ResultsTableCellObject> column = new TableColumn<>(columnTitle);
        column.setCellValueFactory(new MapValueFactory(columnTitle));
        column.setSortable(false);
        final MainController mainController = this;
        column.setCellFactory(new Callback<TableColumn<Map, ResultsTableCellObject>, TableCell<Map, ResultsTableCellObject>>() {
            @Override
            public TableCell call(TableColumn p) {
                TableCell cell = new TableCell<String, ResultsTableCellObject>() {
                    @Override
                    public void updateItem(final ResultsTableCellObject item, final boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item.getItem());
                            if (item.getItem().startsWith("Group ")) {
                                setStyle("-fx-font-weight:bold;");
                            } else if (item.getResult() != null && item.getResult().getTask().getReceivedDate().compareTo(item.getResult().getTask().getDeadline()) > 0) {
                                setStyle("-fx-background-color: " + (item.getResult().getTask().hasHardDeadline() ? "red" : "orange") + ";");
                            } else setStyle("");

                            if (item.getResult() != null) {
                                final ContextMenu contextMenu = new ContextMenu();
                                MenuItem code = new MenuItem("Open code");
                                MenuItem log = new MenuItem("Open log");
                                final CheckMenuItem recheckTask = new CheckMenuItem("Recheck this task");
                                contextMenu.getItems().addAll(code, log, recheckTask);
                                code.setOnAction(event -> {
                                    String command = (System.getProperty("os.name").startsWith("Windows") ? "cmd /C start " : "xdg-open ")
                                            + item.getResult().getTask().getSourcePath();
                                    try {
                                        Runtime.getRuntime().exec(command);
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                });
                                log.setOnAction(event -> {
                                    String sourcePath = item.getResult().getTask().getSourcePath();
                                    sourcePath = sourcePath.substring(0, sourcePath.lastIndexOf('.')) + "Output.txt";
                                    String command = (System.getProperty("os.name").startsWith("Windows") ? "cmd /C start " : "xdg-open ") + sourcePath;
                                    try {
                                        Runtime.getRuntime().exec(command);
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                });
                                recheckTask.setOnAction(event -> {
                                    if (recheckTask.isSelected()) {
                                        LocalSettings.getUncheckedTasks().add(item.getResult().getTask());
                                        String style = getStyle();
                                        if (style.contains("-fx-font-weight:normal")) setStyle(style.replaceAll("-fx-font-weight:normal", "-fx-font-weight:bold"));
                                        else setStyle(style + "-fx-font-weight:bold;");

                                    }
                                    else {
                                        LocalSettings.getUncheckedTasks().remove(item.getResult().getTask());
                                        String style = getStyle();
                                        if (style.contains("-fx-font-weight:bold")) setStyle(style.replaceAll("-fx-font-weight:bold", "-fx-font-weight:normal"));
                                        else setStyle(style + "-fx-font-weight:normal;");
                                    }
                                    mainController.configureButtons();
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

        configureButtons();
        runTests.setOnAction(event -> {
            setMenuDisabled(true);
            try {
                General.getResults(() -> {
                    setMenuDisabled(false);
                    configureButtons();
                }, this);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        runLocalTasks.setOnAction(event -> {
            setMenuDisabled(true);
            General.runLocalTests(() -> {
                        setMenuDisabled(false);
                        configureButtons();
                    }, this,
                    new ConcurrentLinkedQueue<>(LocalSettings.getInstance().getResults().stream().map(Result::getTask).collect(Collectors.toCollection(ArrayList::new))));
        });
        runFailedTasks.setOnAction(event -> {
            setMenuDisabled(true);
            General.runLocalTests(() -> {
                        setMenuDisabled(false);
                        configureButtons();
                    }, this,
                    LocalSettings.getInstance().getFailedTasks());
        });
        runUncheckedTasks.setOnAction(event -> {
            setMenuDisabled(true);
            General.runLocalTests(() -> {
                        setMenuDisabled(false);
                        configureButtons();
                    }, this,
                    LocalSettings.getUncheckedTasks());
        });
        plagiarismResults.setOnAction(event -> {
            if (LocalSettings.getInstance().getPlagiarismResults().size() > 0) {
                try {
                    plagiarismStage = new Stage();
                    plagiarismStage.setScene(new Scene(new FXMLLoader(getClass().getResource("PlagiarismTable.fxml")).load(), 800, 480));
                    plagiarismStage.setTitle("Plagiarism results");
                    plagiarismStage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "No plagiarism results have been acquired. Run tests to get them.");
                alert.showAndWait();
            }
        });
        settings.setOnAction(event -> {
            settingsFrame = new Stage();
            try {
                settingsFrame.setScene(new Scene(new FXMLLoader(getClass().getResource("Settings.fxml")).load()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            settingsFrame.initModality(Modality.WINDOW_MODAL);
            settingsFrame.initOwner(MainFrame.getPrimaryStage());
            settingsFrame.setTitle("Settings");
            settingsFrame.setOnCloseRequest(e -> new Thread(() -> {
                try {
                    GlobalSettings.saveFile();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                configureButtons();
            }).start());
            settingsFrame.show();
        });
        editTasks.setOnAction(event -> {
            Stage taskEditorFrame = new Stage();
            try {
                taskEditorFrame.setScene(new Scene(new FXMLLoader(getClass().getResource("TaskEditor.fxml")).load()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            taskEditorFrame.initModality(Modality.WINDOW_MODAL);
            taskEditorFrame.initOwner(MainFrame.getPrimaryStage());
            taskEditorFrame.setTitle("Task editor");
            taskEditorFrame.show();
        });
    }

    public void setMenuDisabled(boolean disabled) {
        runTests.setDisable(disabled);
        runLocalTasks.setDisable(disabled);
        runFailedTasks.setDisable(disabled);
        runUncheckedTasks.setDisable(disabled);
        plagiarismResults.setDisable(disabled);
        editTasks.setDisable(disabled);
        settings.setDisable(disabled);
    }

    public void configureButtons() {

        runLocalTasks.setManaged(
            LocalSettings.getInstance().getResults().size() != 0 &&
            !GlobalSettings.getInstance().getSubjectsAndGroups().isEmpty() &&
            GlobalSettings.getInstance().getSubjectsAndGroups().values().stream().noneMatch(ArrayList::isEmpty)
        );
        runFailedTasks.setManaged(
            LocalSettings.getInstance().getFailedTasks().size() != 0 &&
            !GlobalSettings.getInstance().getSubjectsAndGroups().isEmpty() &&
            GlobalSettings.getInstance().getSubjectsAndGroups().values().stream().noneMatch(ArrayList::isEmpty)
        );
        plagiarismResults.setManaged(
            LocalSettings.getInstance().getPlagiarismResults().size() != 0 &&
            !GlobalSettings.getInstance().getSubjectsAndGroups().isEmpty() &&
            GlobalSettings.getInstance().getSubjectsAndGroups().values().stream().noneMatch(ArrayList::isEmpty)
        );
        runUncheckedTasks.setManaged(
                LocalSettings.getUncheckedTasks().size() != 0 &&
                        !GlobalSettings.getInstance().getSubjectsAndGroups().isEmpty() &&
                        GlobalSettings.getInstance().getSubjectsAndGroups().values().stream().noneMatch(ArrayList::isEmpty)
        );
        editTasks.setManaged(!GlobalSettings.getInstance().getSubjectsAndGroups().isEmpty() && GlobalSettings.getInstance().getSubjectsAndGroups().values().stream().noneMatch(ArrayList::isEmpty));
        runTests.setManaged(!GlobalSettings.getInstance().getSubjectsAndGroups().isEmpty() && GlobalSettings.getInstance().getSubjectsAndGroups().values().stream().noneMatch(ArrayList::isEmpty));
    }

    public static void showEmailHandlerWindow(EmailHandlerData emailHandlerData) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainController.class.getResource("EmailHandler.fxml"));
            Parent root = fxmlLoader.load();
            EmailHandlerController emailHandlerController = fxmlLoader.getController();
            emailHandlerController.setUserData(emailHandlerData);
            emailHandler = new Stage();
            emailHandler.setTitle("Email Handler");
            emailHandler.setResizable(false);
            emailHandler.setScene(new Scene(root));
            emailHandler.show();
            emailHandler.setOnCloseRequest(we -> {
                emailHandlerData.getEmailHandlerLatch().countDown();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
