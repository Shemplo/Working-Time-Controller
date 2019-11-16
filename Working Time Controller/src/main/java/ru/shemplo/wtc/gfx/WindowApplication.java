package ru.shemplo.wtc.gfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import ru.shemplo.wtc.RunWTC;

public class WindowApplication extends Application {
    
    @Override
    public void start (Stage stage) throws Exception {
        var url = WindowApplication.class.getResource ("/ru/shemplo/wtc/gfx/fxml/main.fxml");
        Scene scene = new Scene (FXMLLoader.load (url));
        scene.setOnMouseClicked (me -> {
            stage.close ();
        });
        
        stage.setTitle ("Working time controller | version " + RunWTC.VERSION);
        stage.initStyle (StageStyle.UNDECORATED);
        stage.setResizable (false);
        stage.setScene (scene);
        stage.sizeToScene ();
        stage.show ();
        
    }
    
}
