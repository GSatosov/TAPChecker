package View;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Created by Alexander Baranov on 03.06.2017.
 */
public class TaskEditorInputTabController implements Initializable {

    @FXML
    public TextArea inputArea;

    @FXML
    public TabPane outputs;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    boolean isListenerSetted = false;

    public void addOutputsListener(TaskEditorController taskEditorController) {
        if (isListenerSetted) return;
        isListenerSetted = true;
        outputs.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() == -1) return;

            if (newValue.intValue() == outputs.getTabs().size() - 1 && outputs.getTabs().get(newValue.intValue()).getText().equals("+")) {
                taskEditorController.addTabOutput("+", -1).setClosable(false);
                outputs.getTabs().get(newValue.intValue()).setText((newValue.intValue() + 1) + "");
                outputs.getSelectionModel().select(newValue.intValue());
            }
        });
    }
}
