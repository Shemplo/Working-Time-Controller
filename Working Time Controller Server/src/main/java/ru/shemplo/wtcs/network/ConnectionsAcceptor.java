package ru.shemplo.wtcs.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import ru.shemplo.dsau.stuctures.Pair;
import ru.shemplo.dsau.utils.ByteManip;
import ru.shemplo.wtcs.Run;

public class ConnectionsAcceptor implements AutoCloseable {

	private final List <Thread> THREADS = new ArrayList <> ();
	
	private ServerSocket server = null;
	private Thread thread = null;
	
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
						
						if (os != null) {
							try { os.close (); } catch (Exception e) {}
						}
						
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
							throw new SocketTimeoutException ();
						}
						
						PENDING_SOCKETS.add (entry);
						continue;
					}
					
					byte [] bytes = new byte [8];
					is.read (bytes, 0, bytes.length);
					long answer = ByteManip.B2L (bytes);
					
					if ((answer ^ 0xff_ff_ff_ff_ff_ff_ff_ffL) != entry.S) {
						// Handshake failed -> dropping connection
						throw new IOException ("wrong handshake answer");
					}
					
					int id = ID_COUNTER.getAndIncrement ();
					NetworkConnection connection = new BaseNetConnection (id, entry.F);
					READY_CONNECTIONS.add (connection);
				} catch (IOException ioe) {
					// Handshake failed -> dropping connection
					
					try {
						entry.F.close ();
					} catch (Exception e) {}
					
					if (is != null) {
						try { is.close (); } catch (Exception e) {}
					}
					
					continue;
				}
			}
		}
	};
	
	public void open () {
		if (thread != null) { return; }
		
		thread = new Thread (ACCEPTOR_TASK, 
			"Connections-Acceptor-Thread");
		THREADS.add (thread);
		thread.start ();
	}

	@Override
	public void close () throws Exception {
		for (Thread thread : THREADS) {
			if (thread == null) { continue; }
			thread.interrupt ();
		}
		
		for (Thread thread : THREADS) {
			if (thread == null) { continue; }
			try {
				thread.join (5000); // 5 seconds
				System.out.println ("Thread " + thread.getName () + " closed");
			} catch (InterruptedException ie) {
				System.err.println ("Thread " + thread.getName () 
					+ " is not closed: " + ie.getMessage ());
			}
		}
	}
	
}
