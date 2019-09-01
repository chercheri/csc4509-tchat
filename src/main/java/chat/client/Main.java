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
package chat.client;

import static chat.common.Log.GEN;
import static chat.common.Log.LOGGER_NAME_DIFFCAUS;
import static chat.common.Log.DIFFCAUS;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.log4j.Level;

import chat.common.Log;

/**
 * This class is the main of the client chat that runs in a separate process. It
 * configures the client, connects to a chat server, launches a thread for
 * reading chat messages from the standard input, and enters an infinite loop
 * for receiving messages and executing the corresponding action of the client.
 * 
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
	 * The method main of a prototypical client. It checks command line arguments,
	 * creates a client object, and enter an infinite loop that reads a chat message
	 * (in the console) and sends it through the client object. The client object
	 * manages a reference to a full duplex message worker that is responsible for
	 * the communication with the chat server.
	 * 
	 * @param args
	 *            the command line arguments.
	 * @throws IOException
	 *             The exception is thrown in two cases: when reading a message in
	 *             the console and when sending a message to the chat server.
	 */
	public static void main(final String[] args) throws IOException {
		if (args.length != 2) {
			GEN.fatal("usage: java -cp <classpath> chat.client.Main" + " <machine> <port>");
			return;
		}
		Log.configureALogger(LOGGER_NAME_DIFFCAUS, Level.INFO);
		DIFFCAUS.info("client lancé");
		Client client = new Client(args[0], Integer.parseInt(args[1]));
		client.startThreadReadMessagesFromNetwork();
		BufferedReader bufin = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));
		while (!Thread.interrupted()) {
			String consoleMsg = null;
			consoleMsg = bufin.readLine();
			if (consoleMsg == null) {
				break;
			}
			// synchronisation made into the client method
			client.treatConsoleInput(consoleMsg);
		}
	}
}
