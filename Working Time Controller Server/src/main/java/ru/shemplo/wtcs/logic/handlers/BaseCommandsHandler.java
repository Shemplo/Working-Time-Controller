package ru.shemplo.wtcs.logic.handlers;

import static java.nio.charset.StandardCharsets.*;

import java.util.StringTokenizer;

import java.io.IOException;

import ru.shemplo.wtcs.Run;
import ru.shemplo.wtcs.network.NetworkConnection;

public class BaseCommandsHandler {

	public static void handle (byte [] input, NetworkConnection connection) {
		if (input == null || input.length == 0) { return; }
		
		if (input [0] == 0) {
			
		} else if (input [0] == 1) {
			String command = new String (input, 1, input.length - 1, UTF_8);
			StringTokenizer st = new StringTokenizer (command.trim ());
			if (!st.hasMoreTokens ()) { return; }
			
			System.out.println (command);
			
			switch (st.nextToken ()) {
				case "stop":
					Run.close ();
					break;
					
				default: {
					try {
						if (connection != null) {
							connection.write ("Echo: " + command);
						}
					} catch (IOException ioe) {}
				}
			}
		}
	}
	
}
