package View;

import Controller.EmailReceiver;
import Controller.GoogleDriveManager;
import Model.GlobalSettings;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;


/**
 * Created by Arseniy Nazarov on 16.03.17.
 */
public class MainFrame extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    private static Stage primaryStage;

    private static Scene mainScene;

    private static Scene loginScene;

    static Stage getPrimaryStage() {
        return primaryStage;
    }

    private static void setPrimaryStage(Stage primaryStage) {
        MainFrame.primaryStage = primaryStage;
    }

    static Scene getMainScene() {
        return mainScene;
    }

    private static void setMainScene(Scene mainScene) {
        MainFrame.mainScene = mainScene;
    }

    static Scene getLoginScene() {
        return loginScene;
    }

    private static void setLoginScene(Scene loginScene) {
        MainFrame.loginScene = loginScene;
    }

    static void setStageToLogin() {
        primaryStage.setScene(loginScene);
        primaryStage.setResizable(false);
        primaryStage.sizeToScene();
    }

    static void setStagetoMain() {
        primaryStage.setScene(mainScene);
        primaryStage.setResizable(true);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        setPrimaryStage(primaryStage);

        setLoginScene(new Scene(FXMLLoader.load(getClass().getResource("Login.fxml")), 430, 180));
        setMainScene(new Scene(FXMLLoader.load(getClass().getResource("Main.fxml")), 640, 480));

        if (((GlobalSettings.getInstance().getEmail().isEmpty())&(GlobalSettings.getInstance().getPassword().isEmpty())) ||
                (!EmailReceiver.validate(GlobalSettings.getInstance().getEmail(), GlobalSettings.getInstance().getPassword()))) {
            setStageToLogin();
        } else {
            GoogleDriveManager.authorize();
            primaryStage.setScene(mainScene);
        }

        primaryStage.show();
    }
}