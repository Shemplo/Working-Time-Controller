package ru.shemplo.wtc.logic;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import ru.shemplo.dsau.stuctures.OwnedVariable;

public class ProjectDescriptor {
	
	// Actually it's useless because PM is public accessible
	private static final Object OWNER = new Object ();
	
	public OwnedVariable <Integer> IDENTIFIER;
	public OwnedVariable <Boolean> INFINITE;
	public OwnedVariable <String> NAME;
	public OwnedVariable <String> PATH;
	
	public final Duration WORK_TIMEOUT = Duration.ofMinutes (1);
    public final AtomicLong workingPeriod = new AtomicLong (0);
    public Duration workingTime = Duration.ofMillis (0);
	
	public ProjectDescriptor () {
		ProjectsManager manager = ProjectsManager.getInstance ();
		this.IDENTIFIER = new OwnedVariable <> (OWNER, manager);
		IDENTIFIER.write (00, OWNER);
		
		this.INFINITE = new OwnedVariable <> (OWNER, manager);
		INFINITE.write (false, OWNER);
		
		this.NAME = new OwnedVariable <> (OWNER, manager);
		NAME.write ("", OWNER);
		
		this.PATH = new OwnedVariable <> (OWNER, manager);
		PATH.write ("", OWNER);
		
		this.workingTime = Duration.ofMillis (0L);
	}
	
	private final Map <WatchKey, Path> PATHS = new HashMap <> ();
	
	public void buildStructure (WatchService watcher, Path root) throws IOException {
		Files.walkFileTree (root, new SimpleFileVisitor <Path> () {
    	    
    	    @Override
    	    public FileVisitResult preVisitDirectory (Path dir, BasicFileAttributes attrs) 
    		    throws IOException {
    	    	registerDirectory (watcher, dir);
    	        return FileVisitResult.CONTINUE;
    	    }
    	    
    	});
	}
	
	private void registerDirectory (WatchService watcher, Path dir) throws IOException {
		Kind <?> [] events = {ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY};
		WatchKey key = dir.register (watcher, events);
		PATHS.put (key, dir);
	}
	
	public void removePath (WatchKey key) {
		this.PATHS.remove (key);
	}
	
	public Path getPathByKey (WatchKey key) {
		return this.PATHS.get (key);
	}
	
	public void dropStructure () {
		PATHS.clear ();
	}
	
	public String toString () {
		StringBuilder sb = new StringBuilder ();
		sb.append ("name "); sb.append (NAME.read ()); sb.append ("\n");
		sb.append ("path "); sb.append (PATH.read ()); sb.append ("\n");
		sb.append ("time "); sb.append (workingTime.toMillis ()); sb.append ("\n");
		
		return sb.toString ();
	}
	
}
