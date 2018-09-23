package ru.shemplo.wtcs;

import static java.time.Duration.*;

import java.util.Random;

import ru.shemplo.wtcs.logic.ProjectsManager;
import ru.shemplo.wtcs.logic.ServerCore;
import ru.shemplo.wtcs.network.ConnectionsAcceptor;

public class Run {
	
	public static final Random R = new Random ();
	
	public static final long HANDSHAKE_TIMEOUT         = ofSeconds (10).toMillis (),
							 CONNECTION_UPDATE_TIMEOUT = ofMillis (50).toMillis (),
							 CONNECTION_TEST_TIMEOUT   = ofSeconds (10).toMillis ();
	
	private static ConnectionsAcceptor acceptor;
	private static ServerCore core;
	
	public static final ProjectsManager MANAGER = ProjectsManager.getInstance ();
	
	public static void main (String ... args) throws Exception {
		MANAGER.loadProjects ();
		
		acceptor = new ConnectionsAcceptor (163);
		acceptor.open ();
		
		core = new ServerCore (acceptor);
		core.start ();
		
		System.out.println ("Server started");
	}
	
	public static void close () {
		Thread closer = new Thread (() -> {
			MANAGER.dumpProjects ();
			try { core.close (); } 
			catch (Exception e) { e.printStackTrace (); }
		}, "Closing-Thread");
		closer.start ();
	}
	
}
