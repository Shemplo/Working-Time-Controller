package ru.shemplo.wtc.logic;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import ru.shemplo.wtc.Run;

public class ProjectsManager {
	
	private static ProjectsManager keeper;
	
	public static final ProjectsManager getInstance () {
		if (keeper == null) {
			synchronized (ProjectsManager.class) {
				if (keeper == null) {
					keeper = new ProjectsManager ();
				}
			}
		}
		
		return keeper;
	}
	
	private final ConcurrentMap <Integer, ProjectDescriptor> PROJECTS = new ConcurrentHashMap <> ();
	private final ConcurrentMap <String, Object> PATHS = new ConcurrentHashMap <> ();
	
	private static final Path getPath () throws IOException {
		String homeAddress = System.getProperty ("user.home");
		Path home = Paths.get (homeAddress);
		
		if (!Files.isWritable (home)) {
			String message = "Failed to write to user home directory";
			throw new IllegalStateException (message);
		}
		
		Path dir = home.resolve (".wtc");
		if (!Files.exists (dir)) {
			Files.createDirectory (dir);
		}
		
		Path file = dir.resolve (".projects");
		if (!Files.exists (file)) {
			Files.createFile (file);
		}
		
		return file;
	}
	
	public final void loadProjects () throws IOException {
		try (
			BufferedReader br = Files.newBufferedReader (getPath ());
		) {
			ProjectDescriptor current = new ProjectDescriptor ();
			String line = null;
			
			while ((line = br.readLine ()) != null) {
				if (line.length () == 0) {
					bindProject (current);
					
					current = new ProjectDescriptor ();
					continue;
				}
				
				StringTokenizer st = new StringTokenizer (line);
				switch (st.nextToken ().toLowerCase ()) {
					case "name":
						if (st.hasMoreTokens ()) {
							current.NAME.write (st.nextToken (), this);
						}
						break;
					case "path":
						if (st.hasMoreTokens ()) {
							current.PATH.write (st.nextToken (), this);
						}
						break;
					case "time":
						if (st.hasMoreTokens ()) {
							Long tmpTime = Long.parseLong (st.nextToken ());
							current.workingTime = Duration.ofMillis (tmpTime);
						}
						break;
				}
			}
			
			bindProject (current);
		}
		
		/*
		System.out.println (this.getClass ().getSimpleName () 
				+ " loaded " + PROJECTS.size () + " project(s)");
		*/
	}
	
	public void dumpProjects () throws IOException {
		try (
			BufferedWriter bw = Files.newBufferedWriter (getPath());
			PrintWriter pw = new PrintWriter (bw);
		) {
			for (ProjectDescriptor project : PROJECTS.values ()) {
				pw.println (project.toString ());
			}
		}
		
		//System.out.println ("Projects dumped");
	}
	
	public List <ProjectDescriptor> listOfProjects () {
		return new ArrayList <> (PROJECTS.values ());
	}
	
	private final static Object HM_DUMMY = new Object ();
	
	public Integer bindProject (ProjectDescriptor project) {
		if (project == null) { return null; }
		
		String path = project.PATH.readNotNull (""),
			   name = project.NAME.readNotNull ("");
		if (path.length () == 0 || name.length () == 0) {
			return null; 
		}
		
		if (PATHS.put (path, HM_DUMMY) == null) {
			int key = project.hashCode ();
			
			project.IDENTIFIER.write (key, this);
			PROJECTS.put (key, project);
			
			return key;
		}
		
		return null;
	}
	
	public ProjectDescriptor getProject (Integer identifier) {
		if (identifier == null) { return null; }
		return PROJECTS.get (identifier);
	}
	
    private final List <Thread> THREADS = new ArrayList <> ();
    private final AtomicLong LAST_LOOP = new AtomicLong ();
    
    private volatile ProjectDescriptor currentProject;
    
    private final Runnable LISTENER_TASK = () -> {
    	WatchService watcher = Run.getWatcher ();
    	
    	ProjectDescriptor descriptor = currentProject;
    	if (descriptor == null) { return; }
    	
    	while (true) {
    	    WatchKey key;
    	    try {
    	    	key = watcher.take ();
    	    } catch (InterruptedException ie) { return; }
    	    
    	    Path dir = descriptor.getPathByKey (key);
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
            				long time = descriptor.workingPeriod.get (), 
            					 suggest = descriptor.WORK_TIMEOUT.toMillis () / 4;
            			    descriptor.workingPeriod.set (Math.max (time, suggest));
            			    descriptor.buildStructure (watcher, child);
            			}
        		    } catch (IOException ioe) {}
        		} else if (event.kind ().equals (ENTRY_MODIFY)) {
        			long timeout = descriptor.WORK_TIMEOUT.toMillis ();
        		    descriptor.workingPeriod.set (timeout);
        		    // TODO: change comparator
        		}
    	    }
    	    
    	    if (!key.reset ()) { descriptor.removePath (key); }
    	}
    }, CHRONO_TASK = () -> {
    	LAST_LOOP.set (System.currentTimeMillis ());
    	
    	ProjectDescriptor descriptor = currentProject;
    	if (descriptor == null) { return; }
    	
    	while (true) {
    	    long current  = System.currentTimeMillis (),
    	    	 lastLoop = LAST_LOOP.get ();
    	    long period = descriptor.workingPeriod.get (),
    	    	 delta = current - lastLoop;
    		    
    	    if (LAST_LOOP.compareAndSet (lastLoop, current)) {
    	    	descriptor.workingPeriod.compareAndSet (period, Math.max (0, period - delta));
        		if (descriptor.workingPeriod.get () > 0) {
        			descriptor.workingTime = descriptor.workingTime
        										.plusMillis (delta);
        		}
    	    }
    	    
    	    try {
    	    	Thread.sleep (100);
    	    } catch (InterruptedException ie) { return; }
    	}
    };
	
	public void openProject (Integer identifier) {
		ProjectDescriptor descriptor = getProject (identifier);
		if (descriptor == null) { return; }
		
		closeProject (); // Closing current project
		
		String pathValue = descriptor.PATH.read ();
		WatchService watcher = Run.getWatcher ();
		
		try {
			Path path = Paths.get (pathValue);
			if (!Files.exists (path)) { return ;}
			
			descriptor.buildStructure (watcher, path);
			currentProject = descriptor;
			
			Thread t = new Thread (LISTENER_TASK);
	        t.setDaemon (true);
	        THREADS.add (t);
	        
	        t = new Thread (CHRONO_TASK);
	        t.setDaemon (true);
	        THREADS.add (t);
	        
	        for (Thread thread : THREADS) {
	            thread.start ();
	        }
		} catch (IOException ioe) {
			descriptor.dropStructure ();
			System.err.println (ioe);
		}
		
		//System.out.println ("Project opened");
	}
	
	public ProjectDescriptor getCurrentProject () {
		return currentProject;
	}
	
	private void closeProject () {
		ProjectDescriptor descriptor = currentProject;
		if (descriptor == null) { return; }
		
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
		
		descriptor.workingPeriod.set (0);
		descriptor.dropStructure ();
	}
	
}