package ru.shemplo.wtc.scenes;

import static java.lang.ClassLoader.*;

import java.io.IOException;
import java.net.URL;
import java.time.temporal.ChronoUnit;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.shemplo.wtc.Run;
import ru.shemplo.wtc.logic.ProjectDescriptor;
import ru.shemplo.wtc.logic.ProjectsManager;

public class MainScene extends StackPane {
	
	private final ProjectsManager MANAGER;
    
    public MainScene () throws IOException {
    	this.MANAGER = ProjectsManager.getInstance ();
    	
        setPadding (new Insets (5, 10, 5, 10));
        setBackground (Run.LIGHT_GRAY_BG);
        setBorder (Run.DEFAULT_BORDERS);
    }
    
    public static enum SL /* Scene Labels */ {
    	
    	TITLE, NAME_INFO, NAME, PATH, TIME
    	;
    	
    	public final String TYPE = this.getClass ()
    				.getSimpleName ().toLowerCase ();
    	
    	public Label get (Parent context) {
    		String id = name ().toLowerCase () + "_" + TYPE;
    		return (Label) context.lookup ("#" + id);
    	}
    	
    } 
    
    public static enum SB /* Scene Buttons */ {
    	
    	EXIT
    	;
    	
    	public final String TYPE = this.getClass ()
    				.getSimpleName ().toLowerCase ();
    	
    	public Button get (Parent context) {
    		String id = name ().toLowerCase () + "_" + TYPE;
    		return (Button) context.lookup ("#" + id);
    	}
    	
    }
    
    public void init () {
    	Button exit = SB.EXIT.get (this);
    	exit.setOnAction (ae -> {
    		try {
				MANAGER.dumpProjects ();
			} catch (IOException ioe) {
				System.err.println (ioe);
				ae.consume ();
			}
    		
    		Run.getStage ().close ();
    	});
    	
    	Label title = SL.TITLE.get (this);
    	title.setText (Run.TITLE);
    	
    	Label name = SL.NAME_INFO.get (this);
    	name.setTextFill (Color.BLUE);
    	name.setCursor (Cursor.HAND);
    	name.setOnMouseClicked (me -> {
    		try {
    			URL resource = getSystemResource (Run.PROJECTS_SCENE_FXML);
                Scene scene = new Scene (FXMLLoader.load (resource));
                scene.getStylesheets ().clear ();
                
                String mainCSS = getSystemResource (Run.PROJECTS_SCENE_CSS)
                					.toExternalForm ();
                scene.getStylesheets ().add (mainCSS);
                
                Stage stage = new Stage ();
                stage.setScene (scene);
                
                if (scene.getRoot () instanceof ProjectsScene) {
                	ProjectsScene projectsScene = (ProjectsScene) scene.getRoot ();
                    projectsScene.init (stage);
                }
                
                stage.initModality (Modality.APPLICATION_MODAL);
                stage.setTitle ("Projects manager");
                stage.initOwner (Run.getStage ());
                stage.setResizable (false);
                stage.sizeToScene ();
                stage.show ();
    		} catch (IOException ioe) {
    			System.err.println (ioe);
    		}
    	});
    	
    	name = SL.NAME.get (this);
    	name.setTextFill (Color.GRAY);
    	
    	Timeline updater = new Timeline (
        	new KeyFrame (Duration.millis (0), e -> updateGUI ()),
        	new KeyFrame (Duration.millis (50))
        );
        
        updater.setCycleCount (Timeline.INDEFINITE);
        updater.setAutoReverse (false);
        updater.playFromStart ();
    }
    
    public void updateGUI () {
    	ProjectDescriptor project = MANAGER.getCurrentProject ();
    	if (project == null) { return; }
    	
    	final MainScene scene = this;
    	Platform.runLater (() -> {
    		Label name = SL.NAME.get (scene);
    		name.setText (project.NAME.read ());
    		name.setTextFill (Color.BLACK);
    		
    		Label path = SL.PATH.get (scene);
    		path.setText (project.PATH.read ());
    		
    		long secondz = project.workingTime.get (ChronoUnit.SECONDS),
               	 period  = project.workingPeriod.get (),
               	 seconds = secondz % 60,
               	 minutes = (secondz = secondz / 60) % 60,
               	 hours   = (secondz = secondz / 60);
    		String format = String.format ("%02d:%02d:%02d (%02.01fs)", 
    							hours, minutes, seconds, period / 1000.0);
    		Label time = SL.TIME.get (scene);
    		time.setTextFill (period > 0 ? Color.GREEN : Color.RED);
    		time.setText (format);
    		
    		autosize ();
    		Run.getStage ().sizeToScene ();
    	});
    }
    
}
