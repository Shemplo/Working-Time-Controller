package ru.shemplo.wtc;

import java.util.Objects;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import ru.shemplo.wtc.scenes.MainScene;

public class Run extends Application {
    
    public static final String TITLE = "Working time controller";
    
    public static final Border 
        DEFAULT_BORDERS = new Border (
            new BorderStroke (Color.GRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)
        );
    
    public static final Background 
        WHITE_BG      = new Background (new BackgroundFill (Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)),
        LIGHT_GRAY_BG = new Background (new BackgroundFill (Color.rgb (245, 245, 245), CornerRadii.EMPTY, Insets.EMPTY));
    
    public static void main (String [] args) {
        launch (args);
    }
    
    private static Stage stage;
    
    public static Stage getStage () {
        return stage;
    }
    
    private Point2D capture = null;
    
    @Override
    public void start (Stage stage) throws Exception {
        Run.stage = stage;
        
        Scene scene = new Scene (new MainScene ());
        scene.setOnMousePressed (me -> { capture = new Point2D (me.getSceneX (), me.getSceneY ()); });
        scene.setOnMouseDragged (me -> {
            if (!Objects.isNull (capture) && MouseButton.PRIMARY.equals (me.getButton ())) {
                stage.setX (me.getScreenX () - capture.getX ());
                stage.setY (me.getScreenY () - capture.getY ());
            }
        });
        
        stage.initStyle (StageStyle.UNDECORATED);
        stage.setResizable (false);
        stage.setTitle (TITLE);
        stage.setScene (scene);
        stage.show ();
    }
    
}
