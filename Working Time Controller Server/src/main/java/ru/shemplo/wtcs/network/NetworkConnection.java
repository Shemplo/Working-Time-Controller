package ru.shemplo.wtcs.network;


public interface NetworkConnection extends AutoCloseable {
	
	public int getID ();
	
	public boolean isConnected ();
	
	public boolean hasInput ();
	
	public String pollInput ();
	
	public void update ();
	
	public void write (String message);
	
}
