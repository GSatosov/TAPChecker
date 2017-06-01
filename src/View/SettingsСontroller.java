package View;

import Model.GlobalSettings;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * Created by Arseniy Nazarov on 27.04.17.
 */
public class SettingsСontroller implements Initializable {

    //Primary settings tab
    @FXML
    TextField tableLink;
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

    @FXML
    Button logout;

    //Autoresponder settings tab
    @FXML
    ToggleButton autoresponderToggle;

    @FXML
    TextArea letterMask;
    @FXML
    TextArea wrongSubject;
    @FXML
    TextArea wrongGroup;
    @FXML
    TextArea wrongTask;

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

        addSubject.setOnAction(event -> {
            Stage confirm = new Stage();
            confirm.setTitle("Add new subject");
            confirm.initModality(Modality.WINDOW_MODAL);

            TextField newSubject = new TextField();
            Button addNewSubject = new Button("Add");

            addNewSubject.disableProperty().bind(Bindings.isEmpty(newSubject.textProperty()));

            addNewSubject.setOnAction(e -> {
                GlobalSettings.getInstance().getSubjectsAndGroups().put(newSubject.getText(), Arrays.asList());
                subjectList.getItems().add(newSubject.getText());
                confirm.close();
            });

            HBox box = new HBox(newSubject, addNewSubject);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(15));
            box.setSpacing(10);

            confirm.setScene(new Scene(box));
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
                GlobalSettings.getInstance().getSubjectsAndGroups().get(subjectList).add(newGroup.getText());
                groupList.getItems().add(newGroup.getText());
                confirm.close();
            });

            HBox box = new HBox(newGroup, addNewGroup);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(15));
            box.setSpacing(10);

            confirm.setScene(new Scene(box));
            confirm.show();
        });

        generateTableLink.setOnAction(event -> {
            //
        });

        logout.setOnAction(event -> {
            GlobalSettings.getInstance().setPassword("");
            MainController.getSettingsFrame().close();
            MainFrame.setStageToLogin();
        });

        autoresponderToggle.selectedProperty().setValue(true);

        autoresponderToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue) {
                letterMask.setDisable(true);
                wrongGroup.setDisable(true);
                wrongSubject.setDisable(true);
                wrongTask.setDisable(true);
            } else {
                letterMask.setDisable(false);
                wrongGroup.setDisable(false);
                wrongSubject.setDisable(false);
                wrongTask.setDisable(false);
            }
        });

        close.setOnAction(event -> ((Stage)(((Button)event.getSource()).getScene().getWindow())).close());
    }
}
