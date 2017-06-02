package View;

import Model.GlobalSettings;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Arseniy Nazarov on 27.04.17.
 */
public class SettingsСontroller implements Initializable {

    //Primary settings tab
    @FXML
    TextField tableLink;
    @FXML
    Button addLink;
    @FXML
    Button generateTableLink;

    @FXML
    ChoiceBox<String> subjectList;
    @FXML
    ChoiceBox<String> groupList;

    @FXML
    Button addSubject;
    @FXML
    Button removeSubject;

    @FXML
    Button addGroup;
    @FXML
    Button removeGroup;

    //Autoresponder settings tab
    @FXML
    ToggleButton autoresponderToggle;

    @FXML
    TextArea emailTemplate;
    @FXML
    TextArea wrongSubject;
    @FXML
    TextArea wrongGroup;
    @FXML
    TextArea wrongTask;

    @FXML
    Button addMasks;

    @FXML
    Button close;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        subjectList.getItems().addAll(GlobalSettings.getInstance().getSubjectsAndGroups().keySet());
        subjectList.setValue(subjectList.getItems().get(0));
        groupList.getItems().addAll(GlobalSettings.getInstance().getSubjectsAndGroups().get(subjectList.getItems().get(0)));
        groupList.setValue(groupList.getItems().get(0));

        subjectList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            groupList.getItems().clear();
            groupList.getItems().addAll(GlobalSettings.getInstance().getSubjectsAndGroups().get(newValue));
            groupList.setValue(groupList.getItems().get(0));
        });

        tableLink.setText(GlobalSettings.getInstance().getResultsTableURL());

        addLink.setOnAction(event -> {
            Pattern pattern = Pattern.compile("/spreadsheets/d/([a-zA-Z0-9-_]+)");
            Matcher matcher = pattern.matcher(tableLink.getText());
            if (matcher.find()) {
                GlobalSettings.getInstance().setResultsTableURL(tableLink.getText());
            }
            else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Entered incorrect link to the table!");
                alert.showAndWait();
                tableLink.setText("");
            }
        });

        removeSubject.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Warning!");
            alert.setContentText("Delete selected subject?");

            if (alert.showAndWait().get() == ButtonType.OK) {
                GlobalSettings.getInstance().getSubjectsAndGroups().remove(subjectList.getValue());
                subjectList.getItems().remove(subjectList.getValue());
            }
        });

        removeGroup.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Warning!");
            alert.setContentText("Delete selected group?");

            if (alert.showAndWait().get() == ButtonType.OK) {
                GlobalSettings.getInstance().getSubjectsAndGroups().get(subjectList.getValue()).remove(groupList.getValue());
                groupList.getItems().remove(groupList.getValue());
            }
        });

        addSubject.setOnAction((ActionEvent event) -> {
            Stage confirm = new Stage();
            confirm.setTitle("Add new subject");
            confirm.initModality(Modality.WINDOW_MODAL);

            TextField newSubject = new TextField();
            Button addNewSubject = new Button("Add");

            addNewSubject.disableProperty().bind(Bindings.isEmpty(newSubject.textProperty()));

            addNewSubject.setOnAction(e -> {
                GlobalSettings.getInstance().getSubjectsAndGroups().put(newSubject.getText(), new ArrayList<>());
                subjectList.getItems().add(newSubject.getText());
                confirm.close();
            });

            HBox box = new HBox(newSubject, addNewSubject);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(15));
            box.setSpacing(10);

            confirm.setScene(new Scene(box));
            confirm.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ENTER)
                    addNewSubject.fire();
            });
            confirm.show();
        });

        addGroup.setOnAction(event -> {
            Stage confirm = new Stage();
            confirm.setTitle("Add new group");
            confirm.initModality(Modality.WINDOW_MODAL);

            TextField newGroup = new TextField();
            Button addNewGroup = new Button("Add");

            addNewGroup.disableProperty().bind(Bindings.isEmpty(newGroup.textProperty()));

            addNewGroup.setOnAction(e -> {
                GlobalSettings.getInstance().getSubjectsAndGroups().get(subjectList.getValue()).add(newGroup.getText());
                groupList.getItems().add(newGroup.getText());
                confirm.close();
            });

            HBox box = new HBox(newGroup, addNewGroup);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(15));
            box.setSpacing(10);

            confirm.setScene(new Scene(box));
            confirm.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ENTER)
                    addNewGroup.fire();
            });
            confirm.show();
        });

        generateTableLink.setOnAction(event -> {
            //
        });

        /*autoresponderToggle.setSelected(GlobalSettings.getInstance().getMasksOn());

        emailTemplate.getParagraphs().addAll(GlobalSettings.getInstance().getLetterMask());
        wrongGroup.getParagraphs().addAll(GlobalSettings.getInstance().getWrongGroup());
        wrongSubject.getParagraphs().addAll(GlobalSettings.getInstance().getWrongSubject());
        wrongTask.getParagraphs().addAll(GlobalSettings.getInstance().getWrongTask());

        //shouldn't work
        addMasks.setOnAction(event -> {
            GlobalSettings.getInstance().setLetterMask(new ArrayList<>((Collection<? extends String>) emailTemplate.getParagraphs()));
            GlobalSettings.getInstance().setWrongGroup(new ArrayList<>((Collection<? extends String>) wrongGroup.getParagraphs()));
            GlobalSettings.getInstance().setWrongSubject(new ArrayList<>((Collection<? extends String>) wrongSubject.getParagraphs()));
            GlobalSettings.getInstance().setWrongTask(new ArrayList<>((Collection<? extends String>) wrongTask.getParagraphs()));
        });*/

        close.setOnAction(event -> {
            ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
            try {
                GlobalSettings.saveFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
    }
}
