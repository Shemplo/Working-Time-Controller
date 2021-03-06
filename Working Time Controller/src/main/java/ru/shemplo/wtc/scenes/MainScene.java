package ru.shemplo.wtc.scenes;

import static java.lang.ClassLoader.*;

import java.util.Locale;

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
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import ru.shemplo.wtc.Run;
import ru.shemplo.wtc.logic.ProjectDescriptor;
import ru.shemplo.wtc.logic.ProjectsManager;
import ru.shemplo.wtc.network.NetworkManager;

public class MainScene extends StackPane {
	
	private final ProjectsManager MANAGER;
	private final NetworkManager NETWORK;
	
    public MainScene () throws IOException {
    	this.MANAGER = ProjectsManager.getInstance ();
    	this.NETWORK = NetworkManager.getInstance ();
    	
        setPadding (new Insets (5, 10, 5, 10));
        setBackground (Run.LIGHT_GRAY_BG);
        setBorder (Run.DEFAULT_BORDERS);
    }
    
    public static enum SL /* Scene Labels */ {
    	
    	TITLE, NAME, PATH, STAT, TIME
    	;
    	
    	public final String TYPE = this.getClass ()
    				.getSimpleName ().toLowerCase ();
    	
    	public Label get (Parent context) {
    		String id = name ().toLowerCase () + "_" + TYPE;
    		return (Label) context.lookup ("#" + id);
    	}
    	
    } 
    
    public static enum SB /* Scene Buttons */ {
    	
    	INFINITY, STOP, EXIT
    	;
    	
    	public final String TYPE = this.getClass ()
    				.getSimpleName ().toLowerCase ();
    	
    	public Button get (Parent context) {
    		String id = name ().toLowerCase () + "_" + TYPE;
    		return (Button) context.lookup ("#" + id);
    	}
    	
    }
    
    public static enum SIV /* Scene ImageViews */ {
    	
    	NETWORK
    	;
    	
    	public final String TYPE = this.getClass ()
    				.getSimpleName ().toLowerCase ();
    	
    	public ImageView get (Parent context) {
    		String id = name ().toLowerCase () + "_" + TYPE;
    		return (ImageView) context.lookup ("#" + id);
    	}
    	
    }
    
    public void init () {
    	/*
    	Button connect = SB.CONNECT.get (this);
    	connect.setOnMouseClicked (me -> {
    		if (NETWORK.isConnected ()) {
    			try {
    				NETWORK.close ();
    			} catch (Exception e) {
    				e.printStackTrace ();
    			}
    		} else {
    			try {
        			NETWORK.connect ("localhost", 163, "shemplo");
        		} catch (IOException ioe) {
        			ioe.printStackTrace ();
        		}
    		}
    	});
    	*/
    	
    	Button inf = SB.INFINITY.get (this);
    	inf.setFocusTraversable (false);
    	inf.setOnAction (ae -> {
    		boolean infinite = MANAGER.isInfinite ();
    		if (infinite) {
    			MANAGER.stopStopwatch ();
    		} else {
    			MANAGER.setInfinite ();
    		}
    	});
    	
    	Button stop = SB.STOP.get (this);
    	stop.setFocusTraversable (false);
    	stop.setOnAction (ae -> MANAGER.stopStopwatch ());
    	
    	Button exit = SB.EXIT.get (this);
    	exit.setFocusTraversable (false);
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
    	title.setTextFill (Color.GRAY);
    	title.setCursor (Cursor.HAND);
    	title.setOnMouseClicked (me -> {
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
    	
    	ImageView network = SIV.NETWORK.get (this);
    	network.setImage (Run.NETWORK_RED);
    	network.setCursor (Cursor.HAND);
    	network.setOnMouseClicked (me -> {
    		if (NETWORK.isConnected ()) {
    			try {
    				NETWORK.close ();
    			} catch (Exception e) {
    				e.printStackTrace ();
    			}
    		} else {
    			try {
        			NETWORK.connect ("localhost", 163, "shemplo");
        		} catch (IOException ioe) {
        			ioe.printStackTrace ();
        		}
    		}
    	});
    	
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
    	final MainScene scene = this;
    	
    	if (project == null) {
    		Platform.runLater (() -> {
    			Button inf = SB.INFINITY.get (scene);
        		inf.setVisible (false);
        		
        		Button stop = SB.STOP.get (scene);
        		stop.setVisible (false);
        		
        		ImageView network = SIV.NETWORK.get (this);
            	network.setImage (NETWORK.isConnected () 
            		? Run.NETWORK_GREEN : Run.NETWORK_RED);
    		});
    		
    		return;
    	}
    	
    	Platform.runLater (() -> {
    		boolean infinite = MANAGER.isInfinite ();
    		Button inf = SB.INFINITY.get (scene);
    		inf.setText (infinite ? "." : ">");
    		inf.setVisible (!infinite);
    		
    		Button stop = SB.STOP.get (scene);
    		stop.setVisible (true);
    		
    		Label title = SL.TITLE.get (scene);
    		title.setText (project.NAME.read ());
    		title.setTextFill (Color.BLACK);
    		
    		Label path = SL.PATH.get (scene);
    		path.setText (project.PATH.read ());
    		
    		Label stat = SL.STAT.get (scene);
    		int dirs = MANAGER.getNumberOfDirectories ();
    		stat.setText ("In project: " + dirs + " tracking entr" 
    							+ (dirs == 1 ? "y" : "ies"));
    		
    		ImageView network = SIV.NETWORK.get (this);
        	network.setImage (NETWORK.isConnected () 
        		? Run.NETWORK_GREEN : Run.NETWORK_RED);
    		
    		long secondz = project.workingTime.get (ChronoUnit.SECONDS),
               	 seconds = secondz % 60,
               	 minutes = (secondz = secondz / 60) % 60,
               	 hours   = (secondz = secondz / 60);
    		double period = project.workingPeriod.get () / 1000.0;
    		period = infinite ? Double.POSITIVE_INFINITY : period;
    		String format = String.format (Locale.ENGLISH, "%02d:%02d:%02d", 
    										hours, minutes, seconds);
    		Label time = SL.TIME.get (scene);
    		time.setTextFill (period > 0 ? Color.GREEN : Color.RED);
    		time.setText (format);
    		
    		autosize ();
    		Run.getStage ().sizeToScene ();
    	});
    }
    
}
