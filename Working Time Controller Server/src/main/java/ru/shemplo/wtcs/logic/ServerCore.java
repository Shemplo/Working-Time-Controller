package ru.shemplo.wtcs.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import java.io.IOException;

import ru.shemplo.wtcs.logic.handlers.BaseCommandsHandler;
import ru.shemplo.wtcs.network.ConnectionsAcceptor;
import ru.shemplo.wtcs.network.NetworkConnection;

public class ServerCore implements AutoCloseable {

	private static ServerCore controller;
	
	public static ServerCore getInstance (ConnectionsAcceptor acceptor) {
		if (controller == null) {
			synchronized (ServerCore.class) {
				if (controller == null) {
					controller = new ServerCore (acceptor);
				}
			}
		}
		
		return controller;
	}
	
	private final Map <Integer, NetworkConnection> CLIENTS = new ConcurrentHashMap <> ();
	private final Map <String, Thread> THREADS = new HashMap <> ();
	private ConnectionsAcceptor acceptor = null;
	
	public ServerCore (ConnectionsAcceptor acceptor) {
		this.acceptor = acceptor;
	}
	
	private final AtomicBoolean IS_CONSOLE_FREE = new AtomicBoolean (true);
	
	private final Runnable CORE_TASK = () -> {
		long start = 0, delta = 0;
		
		while (true) {
			start = System.nanoTime ();
			
			for (Integer key : CLIENTS.keySet ()) {
				NetworkConnection connection = CLIENTS.get (key);
				if (connection == null) { continue; }
				
				if (!connection.isConnected () 
					&& !connection.hasInput ()) {
					try {
						connection.close ();
					} catch (Exception e) {}
					CLIENTS.remove (key);
					
					System.out.println ("Remove dead connection: " + connection);
					continue;
				}
				
				connection.update ();
				byte [] input = null;
				
				while ((input = connection.pollInput ()) != null) {
					BaseCommandsHandler.handle (input, connection);
				}
			}
			
			NetworkConnection connection = null;
			while ((connection = acceptor.pollReady ()) != null) {
				int identifier = connection.getID ();
				
				NetworkConnection prev = CLIENTS.put (identifier, connection);
				if (prev != null) { // Unlucky previous connection
					try { prev.close (); } catch (Exception e) {}
				}
			}
			
			try {
				if (IS_CONSOLE_FREE.compareAndSet (true, false)) {
					int available = System.in.available ();
					if (available > 0) {
						byte [] buffer = new byte [available + 1];
						System.in.read (buffer, 1, available);
						buffer [0] = 1;
						
						BaseCommandsHandler.handle (buffer, null);
					}
					
					IS_CONSOLE_FREE.set (true);
				}
			} catch (IOException ioe) { continue; }
			
			delta = (System.nanoTime () - start) / 1_000_000;
			if (delta < 50) { // 50 milliseconds timeout
				try {
					Thread.sleep (50);
				} catch (InterruptedException ie) {
					return;
				}
			}
		}
	};
	
	public void start () {
		synchronized (THREADS) {
			for (int i = 0; i < 1; i++) {
				String name = "Core-Thread-" + (i + 1);
				Thread thread = new Thread (CORE_TASK, name);
				THREADS.putIfAbsent (name, thread);
				thread.start ();
			}
		}
	}

	@Override
	public void close () throws Exception {
		// Server is stopping -> no input connections
		acceptor.close ();
		
		synchronized (THREADS) {
			for (Thread thread : THREADS.values ()) {
				if (thread == null) { continue; }
				
				thread.interrupt ();
			}
			
			for (Thread thread : THREADS.values ()) {
				if (thread == null) { continue; }
				
				try {
					thread.join (5000); // 5 seconds
					THREADS.remove (thread.getName ());
					System.out.println ("Thread " + thread.getName () + " closed");
				} catch (InterruptedException ie) {
					System.err.println ("Thread " + thread.getName () 
						+ " is not closed: " + ie.getMessage ());
				}
			}
		}
		
		for (NetworkConnection connection : CLIENTS.values ()) {
			connection.close ();
		}
	}
	
}
