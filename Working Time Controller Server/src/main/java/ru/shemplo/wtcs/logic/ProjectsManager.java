package ru.shemplo.wtcs.logic;

import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
	
	private final ConcurrentMap <String, ConcurrentMap <String, ProjectDescriptor>> 
		PROJECTS = new ConcurrentHashMap <> ();
	
	private static final Path getPath () throws IOException {
		Path dir = Paths.get (".").toAbsolutePath ().normalize ();
		
		if (!Files.isWritable (dir)) {
			String message = "Failed to write to current directory";
			throw new IllegalStateException (message);
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
				StringBuilder sb = new StringBuilder ();
				switch (st.nextToken ().toLowerCase ()) {
					case "login":
						while (st.hasMoreTokens ()) {
							sb.append (st.nextToken ());
							sb.append (" ");
						}
						String name = sb.toString ().trim ();
						current.LOGIN.write (name, this);
						break;
						
					case "project":
						while (st.hasMoreTokens ()) {
							sb.append (st.nextToken ());
							sb.append (" ");
						}
						
						String path = sb.toString ().trim ();
						current.PROJECT.write (path, this);
						break;
						
					case "time":
						if (st.hasMoreTokens ()) {
							Long tmpTime = Long.parseLong (st.nextToken ());
							current.TIME.write (tmpTime, this);
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
	
	public void dumpProjects () {
		try (
			BufferedWriter bw = Files.newBufferedWriter (getPath ());
			PrintWriter pw = new PrintWriter (bw);
		) {
			for (ConcurrentMap <String, ProjectDescriptor> 
					project : PROJECTS.values ()) {
				Set <String> names = PROJECTS.keySet ();
				for (String name: names) {
					ProjectDescriptor descriptor = project.get (name);
					if (descriptor == null) { continue; }
					pw.println (descriptor.toString ());
				}
			}
		} catch (IOException ioe) {
			System.err.println (ioe);
		}
		
		System.out.println ("Projects dumped");
	}
	
	public void bindProject (ProjectDescriptor project) {
		if (project == null) { return; }
		
		String name  = project.PROJECT.readNotNull (""),
			   login = project.LOGIN.readNotNull ("");
		if (name.length () == 0 || login.length () == 0) {
			return; 
		}
		
		PROJECTS.putIfAbsent (login, new ConcurrentHashMap <> ());
		PROJECTS.get (login).putIfAbsent (name, project);
	}
	
	public void unbindProject (String login, String project) {
		if (PROJECTS.containsKey (login)) {
			PROJECTS.get (login).remove (project);
		}
	}
	
	public void updateProject (String login, String project, long time) {
		if (!PROJECTS.containsKey (login)) {
			ProjectDescriptor descriptor = new ProjectDescriptor ();
			descriptor.PROJECT.write (project, this);
			descriptor.LOGIN.write (login, this);
			descriptor.TIME.write (time, this);
			bindProject (descriptor);
		}
		
		ProjectDescriptor descriptor = PROJECTS.get (login).get (project);
		if (descriptor != null) {
			long tmp = descriptor.TIME.read ();
			descriptor.TIME.write (Math.max (time, tmp), this);
		}
	}
	
}
