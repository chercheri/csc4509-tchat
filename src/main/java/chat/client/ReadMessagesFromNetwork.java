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

Initial developer(s): Denis Conan, Christian Bac
Contributor(s):
 */
package chat.client;

import static chat.common.Log.COMM;
import static chat.common.Log.GEN;
import static chat.common.Log.LOG_ON;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Objects;

import chat.client.algorithms.Algorithm;
import chat.common.FullDuplexMsgWorker;
import chat.common.Log;
import chat.common.ReadMessageStatus;

/**
 * This class contains the chat client's thread waiting for messages from its
 * server. The constructor initialises the full message worker and the method
 * {@code run} receives messages from the server and dispatch them according to
 * the message type using the method {@code execute} of the class
 * {@link chat.client.algorithms.Algorithm}
 * 
 * @author chris
 * @author Denis Conan
 * 
 */
public class ReadMessagesFromNetwork extends FullDuplexMsgWorker implements Runnable {
	/**
	 * the reference to the client.
	 */
	private final Client client;

	/**
	 * constructs the thread of a client that is responsible for the reception of
	 * messages from the chat server. This thread is then a full duplex message
	 * worker. After the construction of the full message worker, the constructor
	 * receive its first message from the chat server that contains the identity of
	 * the server.
	 * 
	 * @param chan   the socket channel connecting the client to the server.
	 * @param client the reference to the client.
	 */
	ReadMessagesFromNetwork(final SocketChannel chan, final Client client) {
		super(chan);
		Objects.requireNonNull(client, "argument client cannot be null");
		this.client = client;
		ReadMessageStatus msgState;
		do {
			msgState = readMessage();
		} while (msgState != ReadMessageStatus.READDATACOMPLETED);
		try {
			synchronized (this.client) {
				client.setIdentity(getData().filter(data -> data instanceof Integer).map(data -> (Integer) data)
						.orElseThrow(() -> new IOException(
								Log.computeClientLogMessage(client, " was waiting for an integer"))));
			}
		} catch (IOException e) {
			throw new IllegalStateException("communication problem while getting" + " the identity of the chat server");
		}
	}

	/**
	 * organises an infinite loop to receive messages from the chat server and to
	 * execute the corresponding action. The action is searched for in the
	 * enumeration {@link chat.client.algorithms.Algorithm} through the method
	 * {@link chat.client.algorithms.Algorithm#execute(Entity, int, Object)}.
	 */
	@Override
	public void run() {
		if (LOG_ON && GEN.isDebugEnabled()) {
			GEN.debug(Log.computeClientLogMessage(client, " thread for rcving msgs from the network started"));
		}
		ReadMessageStatus messState;
		while (!Thread.interrupted()) {
			try {
				messState = readMessage();
				if (messState == ReadMessageStatus.CHANNELCLOSED) {
					break;
				} else {
					if (messState == ReadMessageStatus.READDATACOMPLETED) {
						// synchronisation made into the client method that is going to be called
						Algorithm.execute(client, getInType(), getData().orElseThrow(
								() -> new IOException(Log.computeClientLogMessage(client, " receives no data"))));
					}
				}
			} catch (IOException e) {
				COMM.error(e);
				throw new RuntimeException(
						"pb in client.ReadMessagesFromNetwork::getData (" + e.getLocalizedMessage() + ")");
			}
			if (LOG_ON && COMM.isTraceEnabled()) {
				COMM.trace(Log.computeClientLogMessage(client, "End of reception of a message"));
			}
		}
	}
}
