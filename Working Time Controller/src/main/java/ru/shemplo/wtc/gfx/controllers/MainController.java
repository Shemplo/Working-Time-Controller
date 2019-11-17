package ru.shemplo.wtc.gfx.controllers;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import lombok.Setter;

public class MainController implements Initializable {
    
    @FXML private Button exitButton;
    
    @Setter private Stage stage;

    @Override
    public void initialize (URL location, ResourceBundle resources) {
        exitButton.setOnMouseClicked (me -> {
            Optional.ofNullable (stage).ifPresent (Stage::close);
        });
    }

}
