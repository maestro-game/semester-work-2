package controllers;

import javafx.fxml.Initializable;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Sphere;

import java.net.URL;
import java.util.ResourceBundle;

public class GameController implements Initializable {
    public static final int RADIUS = 10;

    public Pane pane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Sphere sphere = new Sphere(10, 10);
        sphere.setId("test");
        pane.getChildren().add(sphere);
    }
}
