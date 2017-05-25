package View;

import Controller.General;
import Model.Result;
import Model.Task;
import Model.Test;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private TabPane plagiary;

    @FXML
    private Button tests;
    @FXML
    private Button switchTables;
    @FXML
    private Button settings;
    @FXML
    private Button editTests;
    private Integer previouslyPressedInputButton;
    private Integer previouslyPressedOutputButton;
    private static Stage settingsFrame;

    static Stage getSettingsFrame() {
        return settingsFrame;
    }

    public void showResults(List<Result> results) {
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
            try {
                General.getResults(() -> tests.setDisable(false), this);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
            settingsFrame.setTitle("GlobalSettings");
            settingsFrame.setResizable(false);
            settingsFrame.show();
        });
        editTests.setOnAction(event -> {
            Stage testEditorStage = new Stage();
            GridPane taskPane = new GridPane();
            BorderPane pane = new BorderPane();
            GridPane testsGridPane = new GridPane();
            TextField taskCodeField = new TextField();
            TextField timeLimitField = new TextField();
            CheckBox antiPlagiarismCheckBox = new CheckBox();
            CheckBox hardDeadlineCheckbox = new CheckBox();
            DatePicker deadlinePicker = new DatePicker();
            deadlinePicker.setConverter(new StringConverter<LocalDate>() {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

                @Override
                public String toString(LocalDate date) {
                    if (date != null) {
                        return dateFormatter.format(date);
                    } else {
                        return "";
                    }
                }

                @Override
                public LocalDate fromString(String string) {
                    if (string != null && !string.isEmpty()) {
                        return LocalDate.parse(string, dateFormatter);
                    } else {
                        return null;
                    }
                }
            });
            previouslyPressedInputButton = 0;
            previouslyPressedOutputButton = 0;
            BorderPane tasksBorderPane = new BorderPane();
            BorderPane testsPane = new BorderPane();
            TextField taskNameField = new TextField();
            TextField taskSubjectField = new TextField();
            taskPane.add(new Text("Enter the name of the function to check"), 0, 1);
            taskPane.add(taskNameField, 1, 1);
            taskPane.add(new Text("Enter the name of the subject"), 0, 2);
            taskPane.add(taskSubjectField, 1, 2);
            tasksBorderPane.setCenter(taskPane);
            tasksBorderPane.setBottom(testsPane);
            testsPane.setCenter(testsGridPane);
            taskPane.add(new Text("Enter the code for this task:"), 0, 3);
            taskPane.add(taskCodeField, 1, 3);
            taskPane.add(new Text("Enter the time limit in ms:"), 0, 4);
            taskPane.add(timeLimitField, 1, 4);
            taskPane.add(new Text("Enter the deadline for this task:"), 0, 5);
            taskPane.add(deadlinePicker, 1, 5);
            taskPane.add(new Text("If the deadline has not been met, will the task net 0 points?"), 0, 6);
            taskPane.add(hardDeadlineCheckbox, 1, 6);
            taskPane.add(new Text("Check the task on plagiarism?"), 0, 7);
            taskPane.add(antiPlagiarismCheckBox, 1, 7);
            TextField inputField = new TextField();
            TextField outputField = new TextField();
            outputField.setPrefWidth(200);
            outputField.setPrefHeight(200);
            inputField.setPrefHeight(200);
            ArrayList<Test> newTests = new ArrayList<>();
            ArrayList<ArrayList<String>> outputVariants = new ArrayList<>();
            outputVariants.add(new ArrayList<>());
            newTests.add(new Test(new ArrayList<>(), outputVariants));
            Button saveTests = new Button("Save Tests");
            saveTests.setOnAction(event1 -> {
                fillCurrentTest(newTests, inputField, outputField);
                ArrayList<ArrayList<String>> newOutputVariants = new ArrayList<>();
                newOutputVariants.add(new ArrayList<>());
                newTests.add(new Test(new ArrayList<>(), newOutputVariants));
                testsPane.setTop(showTestButtons(newTests, inputField, outputField, testsPane));
                previouslyPressedOutputButton = 0;
            });
            inputField.setPrefWidth(200);
            testsPane.setTop(showTestButtons(newTests, inputField, outputField, testsPane));
            BorderPane testButtonsPane = new BorderPane();
            Button saveOutputVariantButton = new Button("Save Output");
            saveOutputVariantButton.setOnAction(event1 -> {
                ArrayList<String> outputVariant = Arrays.stream(outputField.getText().split(System.getProperty("line.separator"))).collect(Collectors.toCollection(ArrayList::new));
                newTests.get(previouslyPressedInputButton).setOutputVariant(outputVariant, previouslyPressedOutputButton);
                newTests.get(previouslyPressedInputButton).addOutputVariant(new ArrayList<>());
                testsPane.setRight(showOutputVariantsButtons(outputField, newTests.get(previouslyPressedInputButton)));
            });
            testButtonsPane.setRight(saveOutputVariantButton);
            testButtonsPane.setCenter(saveTests);
            testsPane.setBottom(testButtonsPane);
            testsPane.setRight(showOutputVariantsButtons(outputField, newTests.get(previouslyPressedInputButton)));
            testsGridPane.add(inputField, 0, 1);
            testsGridPane.add(outputField, 1, 1);
            pane.setCenter(tasksBorderPane);
            Button saveTask = new Button("Save Task");
            saveTask.setOnAction(event1 -> {
                Task task = new Task();
                task.setTestFields(Long.parseLong(timeLimitField.getCharacters().toString()),
                        antiPlagiarismCheckBox.isSelected(),
                        java.sql.Date.valueOf(deadlinePicker.getValue()),
                        taskCodeField.getCharacters().toString(),
                        hardDeadlineCheckbox.isSelected());
                fillCurrentTest(newTests, inputField, outputField);
                task.setTestContents(newTests);
            });
            pane.setBottom(saveTask);
            testEditorStage.setScene(new Scene(pane, 500, 450));
            testEditorStage.setTitle("Task Editor");
            testEditorStage.show();
        });
    }

    private void fillCurrentTest(ArrayList<Test> newTests, TextField inputField, TextField outputField) {
        newTests.get(previouslyPressedInputButton).setInput(Arrays.stream(inputField.getText().split(System.getProperty("line.separator"))).collect(Collectors.toCollection(ArrayList::new)));
        if (newTests.get(previouslyPressedInputButton).getOutputVariants().size() == 0) {
            ArrayList<ArrayList<String>> outputVariants = new ArrayList<>();
            outputVariants.add(Arrays.stream(outputField.getText().split(System.getProperty("line.separator"))).collect(Collectors.toCollection(ArrayList::new)));
            newTests.get(previouslyPressedInputButton).setOutputVariants(outputVariants);
        } else
            newTests.get(previouslyPressedInputButton).setOutputVariant(Arrays.stream(outputField.getText().split(System.getProperty("line.separator"))).collect(Collectors.toCollection(ArrayList::new)),
                    previouslyPressedOutputButton);
    }

    private HBox showTestButtons(ArrayList<Test> tests, TextField input, TextField output, BorderPane pane) {
        HBox testButtons = new HBox();
        ArrayList<Button> buttons = new ArrayList<>();
        for (int i = 0; i < tests.size(); i++) {
            Test test = tests.get(i);
            Button testButton = new Button(Integer.toString(i + 1));
            testButton.setOnAction(event -> {
                Test testToBeUpdated = tests.get(previouslyPressedInputButton);
                testToBeUpdated.setInput(Arrays.stream(input.getText().split(System.getProperty("line.separator"))).collect(Collectors.toCollection(ArrayList::new)));
                testToBeUpdated.setOutputVariant(Arrays.stream(output.getText().split(System.getProperty("line.separator"))).collect(Collectors.toCollection(ArrayList::new)),
                        previouslyPressedOutputButton);
                input.setText(test.getInput().stream().reduce("", (a, b) -> a.concat(b + System.getProperty("line.separator"))));
                output.setText(test.getOutputVariants().get(0).stream().reduce("", (a, b) -> a.concat(b + System.getProperty("line.separator"))));
                pane.setRight(showOutputVariantsButtons(output, test));
                previouslyPressedInputButton = Integer.parseInt(testButton.getText()) - 1;
            });
            buttons.add(testButton);
        }
        testButtons.getChildren().addAll(buttons);
        return testButtons;
    }

    private VBox showOutputVariantsButtons(TextField output, Test test) {
        VBox outputButtons = new VBox();
        ArrayList<Button> buttons = new ArrayList<>();
        ArrayList<ArrayList<String>> outputVariants = test.getOutputVariants();
        for (int i = 0; i < outputVariants.size(); i++) {
            Button button = new Button(Integer.toString(i + 1));
            button.setOnAction(event -> {
                ArrayList<String> outputVariant = Arrays.stream(output.getText().split(System.getProperty("line.separator"))).collect(Collectors.toCollection(ArrayList::new));
                output.setText(outputVariants.get(Integer.parseInt(button.getText()) - 1).stream().reduce("", (a, b) -> a.concat(b + System.getProperty(System.lineSeparator()))));
                test.setOutputVariant(outputVariant, previouslyPressedOutputButton);
                previouslyPressedOutputButton = Integer.parseInt(button.getText()) - 1;
            });
            buttons.add(button);
        }
        outputButtons.getChildren().addAll(buttons);
        return outputButtons;
    }
}
