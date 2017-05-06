package View;

import Model.Settings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;

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
    Button logout;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logout.setOnAction(event -> {
            Settings.getInstance().setPassword("");
            try {
                Settings.getInstance().saveSettings();
            } catch (InvalidKeyException | IOException | NoSuchPaddingException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            MainController.getSettingsFrame().close();
            MainFrame.setStageToLogin();
        });
    }
}
