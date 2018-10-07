package ru.shemplo.wtcs.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.ByteManip;
import ru.shemplo.wtcs.Run;

public class ConnectionsAcceptor implements AutoCloseable {

	private final Map <String, Thread> THREADS = new HashMap <> ();
	
	private ServerSocket server = null;
	
	public final int PORT;
	
	public ConnectionsAcceptor (int port) throws IOException {
		this.PORT = port == -1 ? randomPort () : port;
		this.server = new ServerSocket (PORT, 64);
		server.setSoTimeout (100); // 0.1 second
	}
	
	private static int randomPort () {
		return Run.R.nextInt (65535);
	}
	
	private final Queue <Pair <Socket, Long>> 
		PENDING_SOCKETS = new ConcurrentLinkedQueue <> ();
	private final Queue <NetworkConnection> 
		READY_CONNECTIONS = new ConcurrentLinkedQueue <> ();
	
	private final AtomicInteger ID_COUNTER = new AtomicInteger (0);
	
	private final Runnable ACCEPTOR_TASK = () -> {
		while (true) {
			try {
				Socket socket = server.accept ();
				
				if (socket != null) {
					PENDING_SOCKETS.add (Pair.mp (socket, null));
					System.out.println ("New connection accepted: " + socket);
					continue; // It's important to accept all connections
				}
			} catch (SocketTimeoutException ste) {
				if (Thread.interrupted ()) {
					Thread.currentThread ().interrupt ();
					return; // Stopping this thread
				}
			} catch (IOException ioe) {
				System.err.println (ioe);
				return;
			}
			
			Pair <Socket, Long> entry = null;
			while ((entry = PENDING_SOCKETS.poll ()) != null) {
				if (entry.S == null) {
					// Starting handshake with someone on other side
					long time = System.nanoTime ();
					OutputStream os = null;
					
					try {
						os = entry.F.getOutputStream ();
						byte [] bytes = ByteManip.L2B (time);
						os.write (bytes, 0, bytes.length);
					} catch (IOException ioe) {
						// Handshake failed -> dropping connection
						
						try {
							entry.F.close ();
						} catch (Exception e) {}
						
						continue;
					}
					PENDING_SOCKETS.add (Pair.mp (entry.F, time));
					
					continue;
				}
				
				long time = System.nanoTime ();
				InputStream is = null;
				
				// Converting delta of time to milliseconds
				long overTime = (time - entry.S) / 1_000_000;
				
				try {
					is = entry.F.getInputStream ();
					if (is.available () < 8) {
						if (overTime > Run.HANDSHAKE_TIMEOUT) {
							// Time for handshake expired -> dropping connection
							throw new SocketTimeoutException ("Handshake not finished");
						}
						
						PENDING_SOCKETS.add (entry);
						continue;
					}
					
					byte [] bytes = new byte [8];
					is.read (bytes, 0, bytes.length);
					long answer = ByteManip.B2L (bytes);
					
					if ((answer ^ 0xff_ff_ff_ff_ff_ff_ff_ffL) != entry.S) {
						// Handshake failed -> dropping connection
						throw new IOException ("Wrong handshake answer");
					}
					
					int id = ID_COUNTER.getAndIncrement ();
					NetworkConnection connection = new BaseNetConnection (id, entry.F);
					System.out.println ("New client created: " + connection);
					READY_CONNECTIONS.add (connection);
				} catch (IOException ioe) {
					// Handshake failed -> dropping connection
					ioe.printStackTrace ();
					
					try {
						OutputStream os = entry.F.getOutputStream ();
						os.write (ByteManip.I2B (-1));
						os.flush ();
					} catch (IOException ioee) {}
					
					try {
						entry.F.close ();
					} catch (Exception e) {}
					
					continue;
				}
			}
		}
	};
	
	public NetworkConnection pollReady () {
		return READY_CONNECTIONS.poll ();
	}
	
	public void open () {
		synchronized (THREADS) {
			for (int i = 0; i < 1; i++) {
				String name = "Connections-Acceptor-Thread-" + (i + 1);
				Thread thread = new Thread (ACCEPTOR_TASK, name);
				THREADS.putIfAbsent (name, thread);
				thread.start ();
			}
		}
	}

	@Override
	public void close () throws Exception {
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
		
		for (NetworkConnection connection : READY_CONNECTIONS) {
			connection.close ();
		}
		
		for (Pair <Socket, Long> pair : PENDING_SOCKETS) {
			try {
				OutputStream os = pair.F.getOutputStream ();
				os.write (ByteManip.I2B (-1));
				os.flush ();
			} catch (IOException ioe) {}
			
			
			pair.F.close ();
		}
	}
	
}
