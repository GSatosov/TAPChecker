package View;

import Controller.General;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by Arseniy Nazarov on 20.04.17.
 */
public class MainController implements Initializable {

    @FXML
    private static TabPane results;

    public static void addTab(Tab t) {
        results.getTabs().add(t);
    }

    @FXML
    private TabPane plagiary;

    @FXML
    private Button tests;
    @FXML
    private Button switchTables;
    @FXML
    private Button settings;

    private static Stage settingsFrame;

    public static Stage getSettingsFrame (){
        return settingsFrame;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tests.setOnAction(event -> {
            tests.setDisable(true);
            General.getResults(() -> tests.setDisable(false));
        });

        switchTables.setOnAction(event -> {
            if (results.isVisible()) {
                results.setVisible(false);
                plagiary.setVisible(true);
            } else {
                results.setVisible(true);
                plagiary.setVisible(false);
            }
        });

        settings.setOnAction(event -> {
            settingsFrame = new Stage();
            try {
                settingsFrame.setScene(new Scene(new FXMLLoader(getClass().getResource("Settings.fxml")).load(), 640, 480));
            } catch (IOException e) {
                e.printStackTrace();
            }

            settingsFrame.initModality(Modality.WINDOW_MODAL);
            settingsFrame.initOwner(MainFrame.getPrimaryStage());
            settingsFrame.setTitle("Settings");
            settingsFrame.setResizable(false);
            settingsFrame.show();
        });
    }
}
