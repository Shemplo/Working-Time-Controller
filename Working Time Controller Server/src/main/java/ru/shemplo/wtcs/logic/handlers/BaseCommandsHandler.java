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
			
			String word = st.nextToken ().trim ()
							.toLowerCase ();
			switch (word) {
				case "stop":
					Run.close ();
					break;
					
				case "project": {
					String login = "";
					if (st.hasMoreTokens ()) {
						login = st.nextToken ();
					} else { break; }
					
					long time = 0;
					if (st.hasMoreTokens ()) {
						time = Long.parseLong (st.nextToken ());
					} else { break; }
					
					@SuppressWarnings ("unused")
					int active = 0;
					if (st.hasMoreTokens ()) {
						active = Integer.parseInt (st.nextToken ());
					} else { break; }
					
					String project = "";
					if (st.hasMoreTokens ()) {
						StringBuilder sb = new StringBuilder ();
						while (st.hasMoreTokens ()) {
							sb.append (st.nextToken ());
							sb.append (" ");
						}
						
						project = sb.toString ().trim ();
					} else { break; }
					
					System.out.println ("Updating project " + project);
					Run.MANAGER.updateProject (login, project, time);
				}
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
