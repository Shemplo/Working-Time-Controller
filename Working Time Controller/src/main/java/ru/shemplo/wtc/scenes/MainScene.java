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
    
    private final Duration TRACK_TIMEOUT = Duration.ofSeconds (10),
                           WORK_TIMEOUT  = Duration.ofMinutes (1);
    
    private AtomicLong workingPeriod  = new AtomicLong (0);
    private Duration   workingTime = Duration.ofMillis (0);
    
    private Queue <File> PENDING_FILES = new LinkedList <> (),
                         PENDING_DIRS  = new LinkedList <> ();
    private File trackingDirectory = null;
    
    private ConcurrentMap <String, Long> HASHES = new ConcurrentHashMap <> ();
    
    private final Runnable STOPWATCH_TASK = () -> {
        long lastLoop = System.currentTimeMillis ();
        long crawlerPastTime = 0;
        
        @SuppressWarnings ("unused")
        long s = 0, e = 0;
        boolean isCrawlerRunning = false;
        
        while (true) {
            long current = System.currentTimeMillis (),  
                 period  = workingPeriod.get (),
                 delta = current - lastLoop;
            if (period > 0) {
                workingTime = workingTime.plusMillis (delta);
                
                // At least it won't decrease the active period
                workingPeriod.compareAndSet (period, Math.max (0, period - delta));
            }
            
            File dir = trackingDirectory;
            if (!Objects.isNull (dir)) {
                if (crawlerPastTime >= TRACK_TIMEOUT.toMillis () && !isCrawlerRunning) {
                    crawlerPastTime = 0;
                    
                    synchronized (PENDING_DIRS) {
                        PENDING_DIRS.add (trackingDirectory);
                        PENDING_DIRS.notifyAll ();
                        
                        s = System.currentTimeMillis ();
                        isCrawlerRunning = true;
                    }
                }
                
                if (PENDING_DIRS.isEmpty () && isCrawlerRunning) {
                    e = System.currentTimeMillis ();
                    isCrawlerRunning = false;
                    
                    //String format = "Crawler done word by %d ms";
                    //System.out.println (String.format (format, e - s));
                    //System.out.println ("Pending files: " + PENDING_FILES.size ());
                }
                
                crawlerPastTime += delta;
            }
            
            Platform.runLater (() -> {
                File tmp = trackingDirectory;
                if (!Objects.isNull (tmp)) {
                    NAME_VALUE.setText (tmp.getName ());
                    PATH_VALUE.setText (tmp.getAbsolutePath ());
                    autosize ();
                    
                    Run.stage.sizeToScene ();
                }
                
                long sec = workingTime.get (ChronoUnit.SECONDS);
                TIME_VALUE.setText (sec + "s");
                
                if (period > 0) {
                    TIME_VALUE.setTextFill (Color.GREEN);
                } else {
                    TIME_VALUE.setTextFill (Color.RED);
                }
            });
            
            try {
                lastLoop = current;
                Thread.sleep (50);
            } catch (InterruptedException ie) {
                System.err.println (ie);
                return;
            }
        }
    }, CRAWLER_TASK = () -> {
        List <File> files = new ArrayList <> (), 
                    dirs = new ArrayList <> ();
        
        while (true) {
            File file = null;
            synchronized (PENDING_DIRS) {
                while (PENDING_DIRS.isEmpty ()) {
                    try {
                        PENDING_DIRS.wait ();
                    } catch (InterruptedException ie) {
                        System.err.println (ie);
                        return;
                    }
                }
                
                file = PENDING_DIRS.poll ();
            }
            
            if (file.isDirectory ()) {
                files.clear (); dirs.clear ();
                File [] list = file.listFiles ();
                if (Objects.isNull (list)) { System.out.println (file); continue; }
                
                for (File f : list) {
                    if (f == null || !f.exists ()) { continue; }
                    if (f.isDirectory ()) {
                        dirs.add (f);
                    } else if (f.canRead ()) {
                        files.add (f);
                    }
                }
                
                synchronized (PENDING_FILES) {
                    PENDING_FILES.addAll (files);
                    PENDING_FILES.notifyAll ();
                }
                synchronized (PENDING_DIRS) {
                    PENDING_DIRS.addAll (dirs);
                    PENDING_DIRS.notifyAll ();
                }
            } else if (file.canRead ()) {
                // In case of first input path is a file
                synchronized (PENDING_FILES) {
                    PENDING_FILES.add (file);
                    PENDING_FILES.notifyAll ();
                }
            }
        }
    }, COMPARATOR_TASK = () -> {
        /*
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance ("SHA-1");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println (nsae);
            return;
        }
        
        byte [] buffer = new byte [4096];
        */
        
        while (true) {
            File file = null;
            synchronized (PENDING_FILES) {
                while (PENDING_FILES.isEmpty ()) {
                    try {
                        PENDING_FILES.wait ();
                    } catch (InterruptedException ie) {
                        System.err.println (ie);
                        return;
                    }
                }
                
                file = PENDING_FILES.poll ();
            }
            
            /*
            try (
                InputStream is = new FileInputStream (file);
            ) {
                int read = -1;
                while ((read = is.read (buffer, 0, buffer.length)) != -1) {
                    digest.update (buffer, 0, read);
                }
                
                String hash = new String (digest.digest ());
                String fileName = file.getAbsolutePath ();
                if (!hash.equals (HASHES.put (fileName, hash))) {
                    workingPeriod.set (WORK_TIMEOUT.toMillis ());
                }
            } catch (IOException ioe) {
                continue;
            }
            */
            
            String fileName = file.getAbsolutePath ();
            Long modified = file.lastModified ();
            if (!modified.equals (HASHES.put (fileName, modified))) {
                workingPeriod.set (WORK_TIMEOUT.toMillis ());
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
            
            String defaultPath = "C:\\Users\\Public\\workspace_for_prj\\Working Time Controller";
            TextField field = new TextField (defaultPath);
            horizontal.getChildren ().add (field);
            field.setMinWidth (300);
            
            Scene scene = new Scene (vertical);
            Stage stage = new Stage ();
            stage.initModality (Modality.APPLICATION_MODAL);
            stage.setTitle ("Project settings");
            stage.initOwner (Run.stage);
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
    
}
