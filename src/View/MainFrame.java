package View;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;


/**
 * Created by Arseniy Nazarov on 16.03.17.
 */
public class MainFrame extends Application{
    public static void main(String[] args) {
        launch(args);
    }

    private static Stage primaryStage;

    private static Scene mainScene;

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    private static void setPrimaryStage(Stage primaryStage) {
        MainFrame.primaryStage = primaryStage;
    }

    public static Scene getMainScene() {
        return mainScene;
    }

    public static void setMainScene(Scene mainScene) {
        MainFrame.mainScene = mainScene;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        setPrimaryStage(primaryStage);

        Scene loginScene = new Scene(FXMLLoader.load(getClass().getResource("Login.fxml")), 640, 480);
        setMainScene(new Scene(FXMLLoader.load(getClass().getResource("Main.fxml")), 640, 480));

        primaryStage.setScene(loginScene);

        primaryStage.show();
    }
}