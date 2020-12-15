package controllers;

import javafx.fxml.Initializable;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Sphere;

import java.net.URL;
import java.util.ResourceBundle;

public class GameController implements Initializable {
    public Pane pane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Sphere sphere = new Sphere(100);
        sphere.setId("test");
        pane.getChildren().add(sphere);
    }
}
