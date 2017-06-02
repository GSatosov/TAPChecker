package View;

import Controller.EmailReceiver;
import Controller.GoogleDriveManager;
import Model.GlobalSettings;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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

    private static MainController mainController;

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
        primaryStage.setTitle("Login");
        primaryStage.sizeToScene();
    }

    static void setStagetoMain() {
        primaryStage.setScene(mainScene);
        primaryStage.setResizable(true);
        primaryStage.setTitle(GlobalSettings.getApplicationName());
        mainController.showResults();
    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        setPrimaryStage(primaryStage);
        FXMLLoader loaderMain = new FXMLLoader(getClass().getResource("Main.fxml"));
        Parent rootMain = loaderMain.load();
        mainController = (MainController)loaderMain.getController();

        setLoginScene(new Scene(new FXMLLoader(getClass().getResource("Login.fxml")).load()));
        setMainScene(new Scene(rootMain, 640, 480));

        if (((GlobalSettings.getInstance().getEmail().isEmpty())&(GlobalSettings.getInstance().getPassword().isEmpty())) ||
                (!EmailReceiver.validate(GlobalSettings.getInstance().getEmail(), GlobalSettings.getInstance().getPassword()))) {
            setStageToLogin();
        } else {
            GoogleDriveManager.authorize();
            setStagetoMain();
        }

        primaryStage.show();
    }
}