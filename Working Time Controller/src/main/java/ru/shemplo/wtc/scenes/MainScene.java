package ru.shemplo.wtc.scenes;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

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
import ru.shemplo.wtc.structures.CacheLine;
import ru.shemplo.wtc.structures.Pair;

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
    
    private final CacheLine <Long, File> CACHE = new CacheLine <> (1 << 5, Long::compare);
    private final ConcurrentSkipListSet <File> FILES  = new ConcurrentSkipListSet <> ();
    private final ConcurrentMap <String, Long> HASHES = new ConcurrentHashMap <> ();
    
    private final Duration CRAWL_TIMEOUT = Duration.ofSeconds (30),
                           WORK_TIMEOUT  = Duration.ofMinutes (1);
    
    private AtomicLong workingPeriod  = new AtomicLong (0);
    private Duration   workingTime = Duration.ofMillis (0);
    
    private final AtomicBoolean NEED_CRAWL = new AtomicBoolean (false);
    private File trackingDirectory = null;
    private boolean isWorking = false;
    
    private final Consumer <Pair <Long, File>> CACHE_TASK = p -> {
        Long current = p.S.lastModified ();
        Long prev = HASHES.put (p.S.getAbsolutePath (), current);
        if (Objects.isNull (prev) || !current.equals (prev)) {
            workingPeriod.set (WORK_TIMEOUT.toMillis ());
        }
    };
    
    private final Runnable STOPWATCH_TASK = () -> {
        long lastLoop = System.currentTimeMillis (),
             crawlerTimer = 0;
        
        while (true) {
            long current = System.currentTimeMillis (),  
                 period  = workingPeriod.get (),
                 delta = current - lastLoop;
            isWorking = period > 0;
            
            if (isWorking) {
                workingTime = workingTime.plusMillis (delta);
                
                // At least it won't decrease the active period
                workingPeriod.compareAndSet (period, Math.max (0, period - delta));
            }
            
            if (!Objects.isNull (trackingDirectory)) {
                crawlerTimer = Math.max (0, crawlerTimer - delta);
                if (crawlerTimer == 0) {
                    crawlerTimer = CRAWL_TIMEOUT.toMillis ();
                    NEED_CRAWL.set (true);
                }
            }
            
            /////////////
            //// GUI ////
            updateGUI ();
            /////////////
            
            try {
                lastLoop = current;
                Thread.sleep (50);
            } catch (InterruptedException ie) {
                System.err.println (ie);
                return;
            }
        }
    }, CRAWLER_TASK = () -> {
        Queue <File> queue = new LinkedList <> ();
        
        while (true) {
            if (NEED_CRAWL.compareAndSet (true, false)) {
                System.out.println ("Crawler started in " + Thread.currentThread ().getName ());
                File tmp = trackingDirectory;
                if (tmp == null) { continue; }
                
                queue.add (tmp);
                while (!queue.isEmpty ()) {
                    File file = queue.poll ();
                    
                    if (file.isDirectory ()) {
                        File [] list = file.listFiles ();
                        if (list == null) { continue; }
                        
                        for (File temp : list) {
                            queue.add (temp);
                        }
                    } else if (file.canRead () && !FILES.contains (file)) {
                        FILES.add (file);
                    }
                }
                
                System.out.println ("Crawler finished");
                continue;
            }
            
            for (File file : FILES) {
                if (!file.exists () || !file.canRead ()) {
                    FILES.remove (file);
                    break;
                }
                
                Long key = CACHE.getLastKnownMinKey ();
                long modified = file.lastModified ();
                if (key == null || modified > key) {
                    CACHE.insert (modified, file);
                }
            }
            
            try {
                Thread.sleep (100);
            } catch (InterruptedException ie) {
                System.err.println (ie);
                return;
            }
        }
    }, COMPARATOR_TASK = () -> {
        while (true) {
            if (FILES.size () > 0) {
                CACHE.forEach (CACHE_TASK);
            }
            
            try {
                Thread.sleep (50);
            } catch (InterruptedException ie) {
                System.err.println (ie);
                return;
            }
        }
    };
    
    public MainScene () {
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
                String value = field.getText ();
                if (value != null && value.length () > 0) {
                    trackingDirectory = new File (value);
                    workingPeriod.set (WORK_TIMEOUT.toMillis ());
                }
            });
        });
        
        Thread t = new Thread (STOPWATCH_TASK);
        t.setDaemon (true);
        t.start ();
        
        for (int i = 0; i < 2; i++) {
            t = new Thread (CRAWLER_TASK);
            t.setDaemon (true);
            t.start ();
        }
        
        
        for (int i = 0; i < 1; i++) {
            t = new Thread (COMPARATOR_TASK);
            t.setDaemon (true);
            t.start ();
        }   
    }
    
    private void updateGUI () {
        Platform.runLater (() -> {
            File tmp = trackingDirectory;
            if (!Objects.isNull (tmp)) {
                NAME_VALUE.setText (tmp.getName ());
                PATH_VALUE.setText (tmp.getAbsolutePath ());
                autosize ();
                
                Run.getStage ().sizeToScene ();
            }
            
            long sec = workingTime.get (ChronoUnit.SECONDS);
            TIME_VALUE.setText (sec + "s (" + (workingPeriod.get () / 1000.0) + ")");
            
            if (isWorking) {
                TIME_VALUE.setTextFill (Color.GREEN);
            } else {
                TIME_VALUE.setTextFill (Color.RED);
            }
        });
    }
    
}
