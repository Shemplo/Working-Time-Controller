package ru.shemplo.wtc.gfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import ru.shemplo.snowball.utils.MiscUtils;
import ru.shemplo.wtc.RunWTC;
import ru.shemplo.wtc.gfx.controllers.MainController;

public class WindowApplication extends Application {
    
    @Override
    public void start (Stage stage) throws Exception {
        var url = WindowApplication.class.getResource ("/ru/shemplo/wtc/gfx/fxml/main.fxml");
        final var loader = new FXMLLoader (url);
        final var scene = new Scene (loader.load ());
        MiscUtils.<Object, MainController> cast (loader.getController ()).setStage (stage);
        url = WindowApplication.class.getResource ("/ru/shemplo/wtc/css/main.css");
        scene.getStylesheets ().add (url.toExternalForm ());
        
        stage.setTitle ("Working time controller | version " + RunWTC.VERSION);
        stage.initStyle (StageStyle.UNDECORATED);
        stage.setResizable (false);
        stage.setScene (scene);
        stage.sizeToScene ();
        stage.show ();
        
    }
    
}
