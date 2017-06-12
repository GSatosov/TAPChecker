package View;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AddItemController implements Initializable {

    @FXML
    public TextField addItemField;

    @FXML
    public Button addButton;

    @FXML
    public Button cancelButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }
}
