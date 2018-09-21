package ru.shemplo.wtcs.network;

import java.io.IOException;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BaseNetConnection implements NetworkConnection {
	
	private final Socket SOCKET;
	public final int ID;
	
	private final Queue <String> INPUT = new ConcurrentLinkedQueue <> ();
	private volatile boolean isConnected = true;
	
	public BaseNetConnection (int indetifier, Socket socket) {
		this.ID = indetifier; this.SOCKET = socket;
	}
	
	public int getID () { return ID; }

	public boolean isConnected () {
		return isConnected;
	}
	
	public boolean hasInput () {
		return INPUT.size () > 0;
	}
	
	public String pollInput () {
		return INPUT.poll ();
	}
	
	public void update () {
		if (!isConnected ()) { return; }
	}
	
	public void write (String message) {
		
	}
	
	@Override
	public void close () throws Exception {
		isConnected = false;
		
		try {
			SOCKET.close ();
		} catch (IOException ioe) {}
	}
	
}
