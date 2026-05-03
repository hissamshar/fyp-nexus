package com.fyp;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("FYP Management Platform — FAST NUCES Peshawar");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);

        loadView("LoginView");

        stage.show();
    }

    /** Load an FXML view by name and set it as the scene. */
    public static void loadView(String viewName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                Main.class.getResource("/fxml/" + viewName + ".fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                Main.class.getResource("/css/main.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (Exception e) {
            System.err.println("[Main] Failed to load view " + viewName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Load an FXML and return its controller (for passing data). */
    public static <T> T loadViewWithController(String viewName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                Main.class.getResource("/fxml/" + viewName + ".fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                Main.class.getResource("/css/main.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            return loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void loadViewWithContext(String viewName, java.util.function.Consumer<Object> contextSetter) {
        try {
            FXMLLoader loader = new FXMLLoader(
                Main.class.getResource("/fxml/" + viewName + ".fxml"));
            Parent root = loader.load();
            if (contextSetter != null) {
                contextSetter.accept(loader.getController());
            }
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                Main.class.getResource("/css/main.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // No connection pool to shut down — REST API is stateless
    }

    public static void main(String[] args) {
        launch(args);
    }
}
