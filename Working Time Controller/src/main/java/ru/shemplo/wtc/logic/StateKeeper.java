package ru.shemplo.wtc.logic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import ru.shemplo.wtc.structures.OwnedVariable;

public class StateKeeper {
	
	private static StateKeeper keeper;
	
	public static final StateKeeper getInstance () {
		if (keeper == null) {
			synchronized (StateKeeper.class) {
				if (keeper == null) {
					try {
						keeper = new StateKeeper ();
					} catch (IOException ioe) {
						System.err.println (ioe);
					}
				}
			}
		}
		
		return keeper;
	}
	
	private final Map <String, ProjectInfo> PROJECTS = new HashMap <> ();
	
	private StateKeeper () throws IOException {		
		try (
			BufferedReader br = Files.newBufferedReader (getPath ());
		) {
			ProjectInfo current = new ProjectInfo ();
			String line = null;
			
			while ((line = br.readLine ()) != null) {
				if (line.length () == 0) {
					addProject (current);
					
					current = new ProjectInfo ();
					continue;
				}
				
				StringTokenizer st = new StringTokenizer (line);
				switch (st.nextToken ().toLowerCase ()) {
					case "name":
						current.NAME.write (st.nextToken (), current);
						break;
					case "path":
						current.PATH.write (st.nextToken (), current);
						break;
					case "time":
						Long tmpTime = Long.parseLong (st.nextToken ());
						current.TIME.write (tmpTime, current);
						break;
				}
			}
			
			addProject (current);
		}
		
		System.out.println (this.getClass ().getSimpleName () 
				+ " loaded " + PROJECTS.size () + " projects");
	}
	
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
	
	private static class ProjectInfo {
		
		public OwnedVariable <String> NAME;
		public OwnedVariable <String> PATH;
		public OwnedVariable <Long>	  TIME;
		
		public ProjectInfo () {
			this.NAME = new OwnedVariable <> (this);
			NAME.write ("", this);
			
			this.PATH = new OwnedVariable <> (this);
			PATH.write ("", this);
			
			this.TIME = new OwnedVariable<> (this);
			TIME.write (0L, this);
		}
		
		public String toString () {
			StringBuilder sb = new StringBuilder ();
			sb.append ("name "); sb.append (NAME.read ()); sb.append ("\n");
			sb.append ("path "); sb.append (PATH.read ()); sb.append ("\n");
			sb.append ("time "); sb.append (TIME.read ()); sb.append ("\n");
			
			return sb.toString ();
		}
		
	}
	
	public void addProject (ProjectInfo project) {
		String path = project.PATH.readNotNull ("");
		if (path.length () > 0) {
			PROJECTS.put (path, project); 
		}
	}
	
	public void addProject (String path, String name) {
		if (!PROJECTS.containsKey (path)) {
			ProjectInfo project = new ProjectInfo ();
			project.PATH.write (path, project);
			project.NAME.write (name, project);
			
			PROJECTS.put (path, project);
		}
	}
	
	public void updateProject (String path, String name, long time) {
		if (!PROJECTS.containsKey (path)) {
			addProject (path, name);
		}
		
		ProjectInfo project = PROJECTS.get (path);
		project.TIME.write (time, project);
	}
	
	public long getProjectTime (String path) {
		if (!PROJECTS.containsKey (path)) {
			return 0;
		}
		
		return PROJECTS.get (path).TIME.read ();
	}
	
	public void dump () throws IOException {
		try (
			BufferedWriter bw = Files.newBufferedWriter (getPath());
			PrintWriter pw = new PrintWriter (bw);
		) {
			for (ProjectInfo project : PROJECTS.values ()) {
				pw.println (project.toString ());
			}
		}
	}
	
}
