package ru.shemplo.wtc.scenes;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.shemplo.wtc.Run;

public class MainScene extends VBox {
 
    private final HBox PRJ_NAME_HORIZONT = new HBox (),
                       PRJ_PATH_HORIZONT = new HBox (),
                       PRJ_TIME_HORIZONT = new HBox ();
    private final List <HBox> HORIZONTALS = new ArrayList <> ();
    {
        HORIZONTALS.add (PRJ_NAME_HORIZONT);
        HORIZONTALS.add (PRJ_PATH_HORIZONT);
        HORIZONTALS.add (PRJ_TIME_HORIZONT);
        
        Insets margin = new Insets (5, 0, 0, 0);
        for (HBox box : HORIZONTALS) {
            VBox.setMargin (box, margin);
        }
    }
    
    private final Label APP_TITLE  = new Label (Run.TITLE),
                        NAME_INFO  = new Label ("Project name: "),
                        NAME_VALUE = new Label (),
                        PATH_INFO  = new Label ("Project path: "),
                        PATH_VALUE = new Label (),
                        TIME_INFO  = new Label ("Working time: "),
                        TIME_VALUE = new Label ("0s");
    private final List <Label> TITLE_LABELS = new ArrayList <> (),
                               PLAIN_LABELS = new ArrayList <> ();
    {
        TITLE_LABELS.add (APP_TITLE);
        
        PRJ_NAME_HORIZONT.getChildren ().add (NAME_INFO);
        PLAIN_LABELS.add (NAME_INFO);
        
        PRJ_NAME_HORIZONT.getChildren ().add (NAME_VALUE);
        PLAIN_LABELS.add (NAME_VALUE);
        
        PRJ_PATH_HORIZONT.getChildren ().add (PATH_INFO);
        PLAIN_LABELS.add (PATH_INFO);
        
        PRJ_PATH_HORIZONT.getChildren ().add (PATH_VALUE);
        PLAIN_LABELS.add (PATH_VALUE);
        
        PRJ_TIME_HORIZONT.getChildren ().add (TIME_INFO);
        PLAIN_LABELS.add (TIME_INFO);
        
        PRJ_TIME_HORIZONT.getChildren ().add (TIME_VALUE);
        TITLE_LABELS.add (TIME_VALUE);
        
        for (Label node : PLAIN_LABELS) {
            node.setFont (new Font (12));
            node.setWrapText (true);
            node.setMaxWidth (300);
            node.setMinWidth (80);
        }
        
        for (Label node : TITLE_LABELS) {
            node.setFont (new Font (17));
            node.setMinWidth (200);
        }
    }
    
    public void updateGUI () {
	Platform.runLater (() -> {
	    Path tmp = trackingPath;
            if (!Objects.isNull (tmp)) {
                PATH_VALUE.setText (tmp.toAbsolutePath ().toString ());
                NAME_VALUE.setText (tmp.getFileName ().toString ());
                autosize ();
                
                Run.getStage ().sizeToScene ();
            }
            
            long secondz = workingTime.get (ChronoUnit.SECONDS),
        	 period  = workingPeriod.get (),
        	 seconds = secondz % 60,
        	 minutes = (secondz = secondz / 60) % 60,
        	 hours   = (secondz = secondz / 60);
            String format = String.format ("%02d:%02d:%02d (%02.01fs)", 
        	hours, minutes, seconds, period / 1000.0);
            TIME_VALUE.setText (format);
            
            if (period > 0) {
                TIME_VALUE.setTextFill (Color.GREEN);
            } else {
                TIME_VALUE.setTextFill (Color.RED);
            }
	});
    }
    
    private WatchService watcher;
    
    public MainScene () throws IOException {
	this.watcher = FileSystems.getDefault ().newWatchService ();
	
        setPadding (new Insets (5, 10, 5, 10));
        setBackground (Run.LIGHT_GRAY_BG);
        setBorder (Run.DEFAULT_BORDERS);
        
        List <Node> children = getChildren ();
        children.add (APP_TITLE);
        children.add (PRJ_NAME_HORIZONT);
        children.add (PRJ_PATH_HORIZONT);
        children.add (PRJ_TIME_HORIZONT);
        PRJ_TIME_HORIZONT.setAlignment (Pos.BASELINE_LEFT);
        
        NAME_INFO.setTextFill (Color.BLUE);
        NAME_INFO.setCursor (Cursor.HAND);
        NAME_INFO.setOnMouseClicked (me -> {
            VBox vertical = new VBox ();
            vertical.setPadding (new Insets (10, 20, 10, 20));
            vertical.setBackground (Run.LIGHT_GRAY_BG);
            vertical.setBorder (Run.DEFAULT_BORDERS);
            
            HBox horizontal = new HBox ();
            vertical.getChildren ().add (horizontal);
            horizontal.setAlignment (Pos.CENTER_LEFT);
            
            Label label = new Label ("Project path:  ");
            horizontal.getChildren ().add (label);
            
            String defaultPath = "C:\\Users\\Shemp\\git\\Working-Time-Controller";
            TextField field = new TextField (defaultPath);
            horizontal.getChildren ().add (field);
            field.setMinWidth (300);
            
            Scene scene = new Scene (vertical);
            Stage stage = new Stage ();
            stage.initModality (Modality.APPLICATION_MODAL);
            stage.setTitle ("Project settings");
            stage.initOwner (Run.getStage ());
            stage.setResizable (false);
            stage.setScene (scene);
            stage.show ();
            
            stage.setOnCloseRequest (we -> {
                String input = field.getText ().trim ();
                Thread thread = new Thread (() -> {
                    Platform.runLater (() -> setDisable (true));
                    
                    try {
                	loadProject (Paths.get (input));
                    } catch (IOException ioe) {
                	// TODO: display error on GUI
                    }
                    
                    Platform.runLater (() -> setDisable (false));
                });
                thread.start ();
            });
        });
    }
    
    private final Duration WORK_TIMEOUT = Duration.ofMinutes (1);
    private final AtomicLong workingPeriod = new AtomicLong (0);
    private Duration workingTime = Duration.ofMillis (0);
    private Path trackingPath = null;
    
    private final Map <WatchKey, Path> KEYS = new HashMap <> ();
    private final List <Thread> THREADS = new ArrayList <> ();
    private final AtomicLong LAST_LOOP = new AtomicLong ();
    
    private final Runnable LISTENER_TASK = () -> {
	while (true) {
	    WatchKey key;
	    try {
		key = watcher.take ();
	    } catch (InterruptedException ie) { return; }
	    
	    Path dir = KEYS.get (key);
	    if (dir == null) { continue; }
	    
	    for (WatchEvent <?> event : key.pollEvents ()) {
		if (event.kind ().equals (OVERFLOW)) {
		    continue;
		}
		
		@SuppressWarnings ("unchecked")
		WatchEvent <Path> pathEvent = (WatchEvent <Path>) event;
		Path child = dir.resolve (pathEvent.context ());
		
		if (event.kind ().equals (ENTRY_CREATE)) {
		    try {
			if (Files.isDirectory (child, LinkOption.NOFOLLOW_LINKS)) {
			    workingPeriod.set (WORK_TIMEOUT.toMillis () / 4);
			    registerAll (child);
			}
		    } catch (IOException ioe) {}
		} else if (event.kind ().equals (ENTRY_MODIFY)) {
		    workingPeriod.set (WORK_TIMEOUT.toMillis ());
		    // TODO: change comparator
		}
	    }
	    
	    if (!key.reset ()) { KEYS.remove (key); }
	}
    }, CHRONO_TASK = () -> {
	LAST_LOOP.set (System.currentTimeMillis ());
	
	while (true) {
	    long current  = System.currentTimeMillis (),
		 lastLoop = LAST_LOOP.get ();
	    long period = workingPeriod.get ();
	    long delta = current - lastLoop;
		    
	    if (LAST_LOOP.compareAndSet (lastLoop, current)) {
		workingPeriod.compareAndSet (period, Math.max (0, period - delta));
		if (workingPeriod.get () > 0) {
		    workingTime = workingTime.plusMillis (delta);
		}
	    }
	    
	    /* GUI */ updateGUI ();
	    
	    try {
		Thread.sleep (100);
	    } catch (InterruptedException ie) { return; }
	}
    };
    
    private void loadProject (Path root) throws IOException {
	// Closing previous project (if it was opened)
	for (Thread thread : THREADS) {
	    if (thread == null) { continue; }
	    
	    try {
		thread.interrupt ();
		thread.join (1000);
	    } catch (InterruptedException ie) {
		System.err.println (ie);
	    }
	}

	THREADS.clear ();
        KEYS.clear ();
        
        // Starting threads and loading new project
        this.workingPeriod.set (WORK_TIMEOUT.toMillis () / 4);
        this.trackingPath = root;
        
        Thread t = new Thread (LISTENER_TASK);
        t.setDaemon (true);
        THREADS.add (t);
        
        t = new Thread (CHRONO_TASK);
        t.setDaemon (true);
        THREADS.add (t);
        
        registerAll (root);
        
        for (Thread thread : THREADS) {
            thread.start ();
        }
    }
    
    private final void registerAll (Path path) throws IOException {
	Files.walkFileTree (path, new SimpleFileVisitor <Path> () {
	    
	    @Override
	    public FileVisitResult preVisitDirectory (Path dir, BasicFileAttributes attrs) 
		    throws IOException {
		register (dir);
	        return FileVisitResult.CONTINUE;
	    }
	    
	});
    }
    
    private final void register (Path dir) throws IOException {
	KEYS.put (dir.register (watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY), dir);
    }
    
}
