package com.oracleinternship;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load the FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Scene scene = new Scene(loader.load());

        // Load the CSS file
        scene.getStylesheets().add(getClass().getResource("/styles/MainStyle.css").toExternalForm());

        // Get the controller and set the primary stage for fullscreen functionality
        MainAppController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);

        // Set up the stage
        primaryStage.setTitle("Jira to Excel Tool");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(400);
        primaryStage.show();
    }

    // public static void main(String[] args) {
    //     launch(args);
    // }
}
