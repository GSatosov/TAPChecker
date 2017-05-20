package View;

import Model.GlobalSettings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ResourceBundle;

/**
 * Created by Arseniy Nazarov on 27.04.17.
 */
public class SettingsСontroller implements Initializable {
    @FXML
    TextField tableLink;
    @FXML
    Button generateTableLink;
    @FXML
    Button logout;

    @FXML
    Button apply;
    @FXML
    Button ok;
    @FXML
    Button cancel;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        generateTableLink.setOnAction(event -> {
            //
        });
        logout.setOnAction(event -> {
            GlobalSettings.getInstance().setPassword("");
            MainController.getSettingsFrame().close();
            MainFrame.setStageToLogin();
        });
    }
}
