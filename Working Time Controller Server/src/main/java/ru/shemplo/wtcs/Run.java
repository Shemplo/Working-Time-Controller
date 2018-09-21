package ru.shemplo.wtcs;

import static java.time.Duration.*;

import java.util.Random;

import ru.shemplo.wtcs.logic.ServerCore;
import ru.shemplo.wtcs.network.ConnectionsAcceptor;

public class Run {
	
	public static final Random R = new Random ();
	
	public static final long HANDSHAKE_TIMEOUT         = ofSeconds (10).toMillis (),
							 CONNECTION_UPDATE_TIMEOUT = ofMillis (50).toMillis ();
	
	private static ConnectionsAcceptor acceptor;
	private static ServerCore core;
	
	public static void main (String ... args) throws Exception {
		acceptor = new ConnectionsAcceptor (3045);
		acceptor.open ();
		
		core = new ServerCore (acceptor);
		core.start ();
	}
	
	public static void close () {
		Thread closer = new Thread (() -> {
			try { core.close (); } 
			catch (Exception e) { e.printStackTrace (); }
		}, "Closing-Thread");
		closer.start ();
	}
	
}
