package com.currencyexchange.ui.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

public class SceneNavigator {

    public static void navigate(Node sourceNode, String fxmlPath,
                                int width, int height,
                                ApplicationContext context) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();

        Stage stage = (Stage) sourceNode.getScene().getWindow();
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(
            SceneNavigator.class.getResource("/css/styles.css").toExternalForm()
        );

        stage.setScene(scene);
        stage.sizeToScene();
        stage.centerOnScreen();
    }
}
