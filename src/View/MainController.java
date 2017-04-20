package View;

import Controller.General;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by Arseniy Nazarov on 20.04.17.
 */
public class MainController implements Initializable{
    @FXML
    Button tests;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tests.setOnAction(event -> {
            General.main();
        });
    }
}
