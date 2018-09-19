package ru.shemplo.wtc;

import static java.lang.ClassLoader.*;

import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.util.Objects;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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
import ru.shemplo.wtc.logic.ProjectsManager;
import ru.shemplo.wtc.scenes.MainScene;

public class Run extends Application {
    
    public static final String TITLE = "Working time controller";
    
    public static final String MAIN_SCENE_FXML     = "ru/shemplo/wtc/scenes/fxml/main-scene.fxml",
    						   PROJECTS_SCENE_FXML = "ru/shemplo/wtc/scenes/fxml/projects-scene.fxml";
    
    public static final String MAIN_SCENE_CSS     = "ru/shemplo/wtc/scenes/css/main-scene.css",
    						   PROJECTS_SCENE_CSS = "ru/shemplo/wtc/scenes/css/projects-scene.css";
        
    public static final Border 
        DEFAULT_BORDERS = new Border (
            new BorderStroke (Color.GRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)
        );
    
    public static final Background 
        WHITE_BG      = new Background (new BackgroundFill (Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)),
        LIGHT_GRAY_BG = new Background (new BackgroundFill (Color.rgb (245, 245, 245), CornerRadii.EMPTY, Insets.EMPTY));
    
    public static final Image ICON = new Image (Run.class.getResourceAsStream ("gfx/clock.png"));
    
    public static final ProjectsManager MANAGER = ProjectsManager.getInstance ();
    
    public static void main (String [] args) {
        launch (args);
    }
    
    ///////////////////////////////////////////
    private static WatchService watcher = null;
    private static Stage stage;
    ///////////////////////////
    
    public static WatchService getWatcher () {
    	return watcher;
    }
    
    public static Stage getStage () {
        return stage;
    }
    
    private Point2D capture = null;
    
    @Override
    public void start (Stage stage) throws Exception {
    	Run.watcher = FileSystems.getDefault ().newWatchService ();
    	MANAGER.loadProjects ();
        Run.stage = stage;
        
        URL resource = getSystemResource (MAIN_SCENE_FXML);
        Scene scene = new Scene (FXMLLoader.load (resource));
        scene.getStylesheets ().clear ();
        
        String mainCSS = getSystemResource (MAIN_SCENE_CSS).toExternalForm ();
        scene.getStylesheets ().add (mainCSS);
        
        if (scene.getRoot () instanceof MainScene) {
        	MainScene mainScene = (MainScene) scene.getRoot ();
            mainScene.init ();
        }
        
        scene.setOnMousePressed (me -> { capture = new Point2D (me.getSceneX (), me.getSceneY ()); });
        scene.setOnMouseDragged (me -> {
            if (!Objects.isNull (capture) && MouseButton.PRIMARY.equals (me.getButton ())) {
                stage.setX (me.getScreenX () - capture.getX ());
                stage.setY (me.getScreenY () - capture.getY ());
            }
        });
        
        stage.initStyle (StageStyle.UNDECORATED);
        stage.getIcons ().add (ICON);
        stage.setResizable (false);
        stage.setTitle (TITLE);
        stage.setScene (scene);
        stage.show ();
    }
    
}
