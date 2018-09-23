package ru.shemplo.wtc.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.charset.StandardCharsets;

import java.net.Socket;

import ru.shemplo.dsau.utils.ByteManip;

public class NetworkManager implements AutoCloseable {
	
	private static NetworkManager manager;
	
	public static final NetworkManager getInstance () {
		if (manager == null) {
			synchronized (NetworkManager.class) {
				if (manager == null) {
					manager = new NetworkManager ();
				}
			}
		}
		
		return manager;
	}
	
	private static enum State {
		NOT_CONNECTED, CONNECTED, RECONNECTING
	}
	
	private final AtomicReference <State> 
		STATE = new AtomicReference <> (State.NOT_CONNECTED);
	private final AtomicBoolean INPUT  = new AtomicBoolean (true),
								OUTPUT = new AtomicBoolean (true);
	private final Object SIGNAL = new Object ();
	
	private String host, login;
	private int port;
	
	private Socket socket;
	
	private final Queue <byte []> OUTPUT_QUEUE = new ConcurrentLinkedQueue <> ();
	
	private final Runnable NETWORK_TASK = () -> {
		long start = 0, delta = 0;
		
		while (true) {
			start = System.nanoTime ();
			
			State state = STATE.get ();
			if (state == null) {
				synchronized (SIGNAL) {
					while (state != null) {
						try {
							SIGNAL.wait ();
						} catch (InterruptedException ie) {
							return;
						}
					}
				}
				
				continue;
			}
			
			switch (state) {
				case NOT_CONNECTED: {
					try {
						Thread.sleep (100);
					} catch (InterruptedException ie) {
						return;
					}
				}
				break;
				
				case RECONNECTING: {
					if (STATE.compareAndSet (state, null)) {
						if (socket != null && !socket.isClosed ()) {
							try   { socket.close (); } 
							catch (Exception e) {}
						}
						
						try {
							socket = new Socket (host, port);
							InputStream is = socket.getInputStream ();
							
							byte [] buffer = new byte [8];
							is.read (buffer, 0, buffer.length);
							long number = ByteManip.B2L (buffer);
							number ^= 0xFF_FF_FF_FF_FF_FF_FF_FFL;
							buffer = ByteManip.L2B (number);
							
							OutputStream os = socket.getOutputStream ();
							os.write (buffer, 0, buffer.length);
							
							STATE.set (State.CONNECTED);
						} catch (Exception e) {
							STATE.set (State.NOT_CONNECTED);
						}
						
						synchronized (SIGNAL) {
							SIGNAL.notifyAll ();
						}
					} else {
						// Go to new loop to lock on SIGNAL
						// and wait until the reconnection
						continue;
					}
				}
				break;
				
				case CONNECTED: {
					if (INPUT.compareAndSet (true, false)) {
						try {
							InputStream is = socket.getInputStream ();
							while (is.available () >= 4) {
								byte [] sizeBuffer = new byte [4];
								
								is.read (sizeBuffer, 0, sizeBuffer.length);
								int length = ByteManip.B2I (sizeBuffer);
								
								if (length == -1) {
									throw new IOException ("Server closed connection");
								} else if (length == -2) {
									System.out.println ("Test of ping");
								} else {
									byte [] buffer = new byte [length];
									length = is.read (buffer, 0, length);
									
									String input = new String (buffer, 0, length, StandardCharsets.UTF_8);
									System.out.println (input);
								}
							}
						} catch (IOException ioe) {
							STATE.compareAndSet (state, State.NOT_CONNECTED);
							try { socket.close (); } catch (Exception e) {}
							
							INPUT.set (true);
							continue;
						}
						
						INPUT.set (true);
					}
					
					if (OUTPUT.compareAndSet (true, false)) {
						try {
							OutputStream os = socket.getOutputStream ();
							
							byte [] output = null;
							while ((output = OUTPUT_QUEUE.poll ()) != null) {
								if (output.length == 0) { continue; }
								
								os.write (ByteManip.I2B (output.length));
								os.write (output, 0, output.length);
								os.flush ();
							}
						} catch (IOException ioe) {
							STATE.compareAndSet (state, State.NOT_CONNECTED);
							try { socket.close (); } catch (Exception e) {}
						}
						
						OUTPUT.set (true);
					}
				}
				break;
			}
			
			delta = (System.nanoTime () - start) / 1_000_000;
			if (delta < 50) {
				try {
					Thread.sleep (50);
				} catch (InterruptedException ie) {
					return;
				}
			}
		}
	};
	
	private final Map <String, Thread> THREADS = new HashMap <> ();
	
	public NetworkManager () {
		synchronized (THREADS) {
			for (int i = 0; i < 1; i++) {
				String name = "Network-Thread-" + (i + 1);
				Thread thread = new Thread (NETWORK_TASK, name);
				THREADS.putIfAbsent (name, thread);
				thread.setDaemon (true);
				thread.start ();
			}
		}
	}
	
	public void connect (String host, int port, String login) throws IOException {
		if (host == null || host.length () == 0 
			|| login == null || login.length () == 0) {
			return;
		}
		
		this.host = host; this.port = port; this.login = login;
		STATE.set (State.RECONNECTING);
	}
	
	public void connect (String host, int port) throws IOException {
		if (hasProfile ()) { connect (host, port, this.login); }
	}
	
	public boolean hasProfile () {
		return login != null && login.length () > 0;
	}
	
	public String getProfile () {
		return hasProfile () ? login : "";
	}
	
	public boolean isConnected () {
		return State.CONNECTED.equals (STATE.get ());
	}
	
	public void write (byte [] data) {
		if (data == null || data.length == 0) { return; }
		byte [] buffer = new byte [data.length + 1];
		
		System.arraycopy (data, 0, buffer, 1, data.length);
		buffer [0] = 0; // it means raw bytes
		
		OUTPUT_QUEUE.add (buffer);
	}
	
	public void write (String message) {
		if (message == null || message.length () == 0) { return; }
		byte [] data = message.getBytes (StandardCharsets.UTF_8);
		byte [] buffer = new byte [data.length + 1];
		
		System.arraycopy (data, 0, buffer, 1, data.length);
		buffer [0] = 1; // it means string
		
		OUTPUT_QUEUE.add (buffer);
	}

	@Override
	public void close () throws Exception {
		STATE.set (State.NOT_CONNECTED);
		
		try { 
			socket.close (); 
		} catch (Exception e) {}
	}
	
}
