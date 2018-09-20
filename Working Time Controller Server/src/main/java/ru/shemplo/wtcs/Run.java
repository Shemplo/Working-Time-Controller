package ru.shemplo.wtcs;

import java.time.Duration;
import java.util.Random;

import ru.shemplo.wtcs.network.ConnectionsAcceptor;

public class Run {
	
	public static final Random R = new Random ();
	
	public static final long HANDSHAKE_TIMEOUT = Duration.ofSeconds (10).toMillis ();
	
	public static void main (String ... args) throws Exception {
		ConnectionsAcceptor acceptor = new ConnectionsAcceptor (3045);
		acceptor.open ();
		acceptor.close ();
	}
	
}
