package ru.shemplo.wtc.gfx;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import ru.shemplo.snowball.utils.MiscUtils;
import ru.shemplo.wtc.RunWTC;
import ru.shemplo.wtc.gfx.controllers.StageDependent;

public class WindowApplication extends Application {
    
    public static final String RESOURCES_ROOT = "/ru/shemplo/wtc";
    
    public static final String RESOURCES_CSS = RESOURCES_ROOT + "/css";
    public static final String RESOURCES_GFX = RESOURCES_ROOT + "/gfx";
    public static final String RESOURCES_FXML = RESOURCES_GFX + "/fxml";
    
    private Point2D capture = null;
    
    @Override
    public void start (Stage stage) throws Exception {
        final String title = "Working time controller | version " + RunWTC.VERSION;
        createNewWindow (title, stage, null, RESOURCES_FXML + "/main.fxml", List.of (
            RESOURCES_CSS + "/main.css"
        ));
        
        final Scene scene = stage.getScene ();
        scene.setOnMousePressed (me -> { capture = new Point2D (me.getSceneX (), me.getSceneY ()); });
        scene.setOnMouseDragged (me -> {
            if (!Objects.isNull (capture) && MouseButton.PRIMARY.equals (me.getButton ())) {
                stage.setX (me.getScreenX () - capture.getX ());
                stage.setY (me.getScreenY () - capture.getY ());
            }
        });
        
        stage.initStyle (StageStyle.UNDECORATED);
        stage.show ();
    }
    
    public static Stage createNewWindow (String title, Stage stage, Stage owner,
            String layout, List <String> styles) throws IOException {
        if (stage == null) { stage = new Stage (); }
        stage.setTitle (title);
    
        var url = WindowApplication.class.getResource (layout);
        final var loader = new FXMLLoader (url);
    
        final var scene = new Scene (loader.load ());
        MiscUtils.<Object, StageDependent> cast (loader.getController ()).setStage (stage);
        
        for (String style : styles) {
            url = WindowApplication.class.getResource (style);
            scene.getStylesheets ().add (url.toExternalForm ());
        }
        
        if (owner != null) {
            stage.initModality (Modality.APPLICATION_MODAL);
            stage.initOwner (owner);
        }
    
        stage.setResizable (false);
        stage.setScene (scene);
        stage.sizeToScene ();
        
        return stage;
    }
    
}
