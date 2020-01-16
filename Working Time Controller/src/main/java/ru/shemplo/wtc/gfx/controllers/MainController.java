package ru.shemplo.wtc.gfx.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.Setter;
import ru.shemplo.snowball.utils.MiscUtils;
import ru.shemplo.wtc.gfx.WindowApplication;

public class MainController implements Initializable, StageDependent {
    
    @FXML private StackPane root;
    
    @FXML private Button startButton;
    @FXML private Button stopButton;
    
    @FXML private Button settingButton;
    @FXML private Button exitButton;
    
    @FXML private Label projectNameValue;
    @FXML private Label timeCounterValue;
    @FXML private Label projectStructure;
    
    @FXML private ProgressBar timeTimerValue;
    
    @Setter (onMethod_ = {@Override}) private Stage stage;

    @Override
    public void initialize (URL location, ResourceBundle resources) {
        exitButton.setOnMouseClicked (me -> {
            Optional.ofNullable (stage).ifPresent (Stage::close);
        });
        
        var timerParentWidthProps = MiscUtils.<Parent, HBox> cast (timeTimerValue.getParent ())
                                  . widthProperty ();
        timeTimerValue.minWidthProperty ().bind (timerParentWidthProps);
        timeTimerValue.maxWidthProperty ().bind (timerParentWidthProps);
        timeTimerValue.setProgress (0.89);
        
        settingButton.setOnMouseClicked (me -> {
            try {
                String title = "Working time controller | projects manager";
                Stage modalStage = WindowApplication.createNewWindow (title, null, stage,
                    WindowApplication.RESOURCES_FXML + "/settings.fxml",
                    List.of (
                        WindowApplication.RESOURCES_CSS + "/main.css"
                    )
                );
                
                modalStage.show ();
            } catch (IOException ioe) {
                System.err.println (ioe);
            }
        });
    }

}
