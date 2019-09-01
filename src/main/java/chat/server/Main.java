/**
This file is part of the CSC4509 teaching unit.

Copyright (C) 2012-2019 Télécom SudParis

This is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This software platform is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with the CSC4509 teaching unit. If not, see <http://www.gnu.org/licenses/>.

Initial developer(s): Denis Conan
Contributor(s):
 */
package chat.server;

import static chat.common.Log.GEN;
import static chat.common.Log.LOG_ON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import chat.common.Log;

/**
 * This class defines the main of a chat server. It creates and configures the
 * server object, and enters an infinite loop to read command line to control
 * the server.
 * 
 * @author chris
 * @author Denis Conan
 * 
 */
public final class Main {
	/**
	 * private constructor to avoid creating instances.
	 */
	private Main() {
	}

	/**
	 * The method main of a prototypical server. It checks command line arguments,
	 * creates a server object, and enter an infinite loop that reads a server
	 * command (in the console). The server object manages a thread that controls a
	 * collection of full duplex message workers that are responsible for the
	 * communication with the chat clients and the other server. This latter thread
	 * must be started after the creation of the server object.
	 * 
	 * @param args the command line arguments.
	 * @throws IOException The exception is thrown in two cases: when reading a
	 *                     message in the console.
	 */
	public static void main(final String[] args) throws IOException {
		if (LOG_ON && GEN.isTraceEnabled()) {
			GEN.trace("there are " + args.length + " command line arguments");
		}
		if ((args.length < 1) || ((args.length % 2) == 0)) {
			GEN.fatal("usage: java -cp <classpath> chat.server.Main" + " <server number>"
					+ " <list of pairs hostname servernumber>");
			return;
		}
		Server server = new Server(args);
		server.startThreadReadMessagesFromNetwork();
		BufferedReader bufin = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));
		while (!Thread.interrupted()) {
			String consoleMsg = null;
			consoleMsg = bufin.readLine();
			if (consoleMsg == null) {
				break;
			}
			if (LOG_ON && GEN.isDebugEnabled()) {
				GEN.debug(Log.computeServerLogMessage(server, "new command line for server: " + consoleMsg));
			}
			// synchronisation made into the server method
			server.treatConsoleInput(consoleMsg);
		}
	}
}
