package View;

import Controller.General;
import Model.Settings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ResourceBundle;

/**
 * Created by Arseniy Nazarov on 20.04.17.
 */
public class MainController implements Initializable {

    @FXML
    private TableView results;

    @FXML
    private Button tests;
    @FXML
    private Button settings;
    @FXML
    private Button logout;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tests.setOnAction(event -> {
            tests.setDisable(true);
            General.getResults(() -> tests.setDisable(false));
        });

        logout.setOnAction(event -> {
            Settings.getInstance().setPassword("");
            try {
                Settings.getInstance().saveSettings();
            } catch (InvalidKeyException | IOException | NoSuchPaddingException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            MainFrame.getPrimaryStage().setScene(MainFrame.getLoginScene());
        });

        settings.setOnAction(event -> {
            Stage settingsFrame = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Settings.fxml"));
            loader.setController(new SettingsСontroller());
            try {
                settingsFrame.setScene(new Scene(loader.load(), 640, 480));
            } catch (IOException e) {
                e.printStackTrace();
            }

            settingsFrame.show();
        });
    }
}
