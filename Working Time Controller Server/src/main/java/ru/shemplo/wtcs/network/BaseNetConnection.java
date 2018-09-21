package ru.shemplo.wtcs.network;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import ru.shemplo.dsau.utils.ByteManip;
import ru.shemplo.wtcs.Run;
import ru.shemplo.wtcs.logic.BaseMessageReader;
import ru.shemplo.wtcs.logic.MessageReader;

public class BaseNetConnection implements NetworkConnection {
	
	private final Socket SOCKET;
	public final int ID;
	
	private final Queue <String> INPUT = new ConcurrentLinkedQueue <> ();
	private final AtomicBoolean IS_UPDATING = new AtomicBoolean (false);
	private final MessageReader MESSANGER = new BaseMessageReader ();
	
	private volatile boolean isConnected = true;
	private volatile long lastUpdated = 0;
	
	private volatile OutputStream os;
	private volatile InputStream is;
	
	public BaseNetConnection (int indetifier, Socket socket) {
		this.ID = indetifier; this.SOCKET = socket;
		this.lastUpdated = System.nanoTime ();
		
		try {
			os = SOCKET.getOutputStream ();
		} catch (IOException ioe) {}
		
		try {
			is = SOCKET.getInputStream ();
		} catch (IOException ioe) {}
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
		if (is == null && os == null) { isConnected = false; }
		
		if (!isConnected () || is == null) { return; }
		
		long delta = (System.nanoTime () - lastUpdated) / 1_000_000;
		if (delta > Run.CONNECTION_UPDATE_TIMEOUT 
			&& IS_UPDATING.compareAndSet (false, true)) {
			
			try {
				String input = MESSANGER.read (is);
				if (input.length () != 0) { 
					// TODO: send to commands processor
					System.out.println (input);
				}
			} catch (IOException ioe) {
				is = null; return;
			} catch (Exception e) {
				// XXX: it's temporal
				e.printStackTrace ();
			}
			
			lastUpdated = System.nanoTime ();
			IS_UPDATING.set (false);
		}
	}
	
	@Override
	public void write (byte [] data) throws IOException {
		if (data == null || data.length == 0) { return; }
		
		OutputStream os = this.os;
		if (os == null) { return; }
		
		synchronized (os) {
			try {
				byte [] length = ByteManip.I2B (data.length);
				os.write (length, 0, length.length);
				os.write (data, 0, data.length);
				os.flush ();
			} catch (IOException ioe) {
				this.os = null;
				throw ioe;
			}
		}
	}
	
	public void write (String message) throws IOException {
		if (message == null || message.length () == 0) { return; }
		write (message.getBytes (StandardCharsets.UTF_8));
	}
	
	@Override
	public void close () throws Exception {
		isConnected = false;
		
		try {
			OutputStream os = SOCKET.getOutputStream ();
			os.write (ByteManip.I2B (-1));
			os.flush ();
		} catch (IOException ioe) {}
		
		try {
			SOCKET.close ();
		} catch (IOException ioe) {}
	}
	
}
