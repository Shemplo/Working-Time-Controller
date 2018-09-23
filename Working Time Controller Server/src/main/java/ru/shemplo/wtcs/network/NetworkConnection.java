package ru.shemplo.wtcs.network;

import java.io.IOException;

public interface NetworkConnection extends AutoCloseable {
	
	public int getID ();
	
	public boolean isConnected ();
	
	public boolean hasInput ();
	
	public byte [] pollInput ();
	
	public void update ();
	
	public void write (byte [] data) throws IOException;
	
	public void write (String message) throws IOException;
	
}
