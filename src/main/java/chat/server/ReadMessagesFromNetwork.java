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

import static chat.common.Log.COMM;
import static chat.common.Log.GEN;
import static chat.common.Log.LOG_ON;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import chat.client.algorithms.chat.ChatMsgContent;
import chat.common.FullDuplexMsgWorker;
import chat.common.Log;
import chat.common.MsgContent;
import chat.common.ReadMessageStatus;
import chat.server.algorithms.ServerToServerMsgContent;

/**
 * This class defines the main of the chat server. It configures the server,
 * connects to existing chat servers, waits for connections from other chat
 * servers and from chat clients, and forwards chat messages received from chat
 * clients to other 'local' chat clients and to the other chat servers.
 * 
 * The chat servers can be organised into a network topology forming cycles
 * since the method <tt>forward</tt> is only called when the message to forward
 * has not already been received and forwarded.
 * 
 * @author chris
 * @author Denis Conan
 * 
 */
public class ReadMessagesFromNetwork implements Runnable {
	/**
	 * backward reference to the server selector object in order to use its methods
	 * to send messages.
	 */
	private final Server server;

	/**
	 * the selector.
	 */
	private final Selector selector;

	/**
	 * the selection key for accepting client connections.
	 */
	private final SelectionKey acceptClientKey;

	/**
	 * server socket channel for accepting client connections.
	 */
	private final ServerSocketChannel listenChanClient;

	/**
	 * the selection key for accepting server connections.
	 */
	private final SelectionKey acceptServerKey;

	/**
	 * server socket channel for accepting server connections.
	 */
	private final ServerSocketChannel listenChanServer;

	/**
	 * initialises the collection attributes and the state of the server, and
	 * creates the channels that are accepting connections from clients and servers.
	 * 
	 * @param server           the reference to the server.
	 * @param selector         the selector.
	 * @param acceptClientKey  the selection key for accepting client connections.
	 * @param listenChanClient the server socket channel for accepting client
	 *                         connections.
	 * @param acceptServerKey  the selection key for accepting server connections.
	 * @param listenChanServer the server socket channel for accepting server
	 *                         connections.
	 */
	public ReadMessagesFromNetwork(final Server server, final Selector selector, final SelectionKey acceptClientKey,
			final ServerSocketChannel listenChanClient, final SelectionKey acceptServerKey,
			final ServerSocketChannel listenChanServer) {
		Objects.requireNonNull(server, "argument server cannot be null");
		Objects.requireNonNull(selector, "argument selector cannot be null");
		Objects.requireNonNull(acceptClientKey, "argument acceptClientKey cannot be null");
		Objects.requireNonNull(listenChanClient, "argument listenChanClient cannot be null");
		Objects.requireNonNull(acceptServerKey, "argument acceptServerKey cannot be null");
		Objects.requireNonNull(listenChanServer, "argument listenChanServer cannot be null");
		this.selector = selector;
		this.acceptClientKey = acceptClientKey;
		this.listenChanClient = listenChanClient;
		this.acceptServerKey = acceptServerKey;
		this.listenChanServer = listenChanServer;
		this.server = server;
	}

	/**
	 * is the infinite loop organised around the call to select.
	 */
	@Override
	public void run() {
		if (LOG_ON && GEN.isDebugEnabled()) {
			GEN.debug(Log.computeServerLogMessage(server, ", thread for rcving msgs from the network started"));
		}
		while (!Thread.interrupted()) {
			try {
				selector.select();
			} catch (IOException e) {
				COMM.fatal(e.getLocalizedMessage());
				return;
			}
			Set<SelectionKey> readyKeys = selector.selectedKeys();
			Iterator<SelectionKey> readyIter = readyKeys.iterator();
			while (readyIter.hasNext()) {
				SelectionKey key = readyIter.next();
				readyIter.remove();
				if (key.isAcceptable()) {
					try {
						if (key.equals(acceptServerKey)) {
							server.acceptNewServer(listenChanServer);
						} else if (key.equals(acceptClientKey)) {
							server.acceptNewClient(listenChanClient);
						} else {
							COMM.fatal("unknown accept");
							return;
						}
					} catch (IOException e) {
						COMM.error(e.getLocalizedMessage());
					}
				}
				if (key.isReadable()) {
					Optional<FullDuplexMsgWorker> serverWorker = null;
					synchronized (server) {
						server.setSelectionKeyOfCurrentMsg(key);
						serverWorker = server.getServerWorker(key);
						if (serverWorker.isPresent()) {
							treatMessageFromNeighbouringServer(key, serverWorker.get());
						}
					}
					Optional<FullDuplexMsgWorker> clientWorker = null;
					synchronized (server) {
						clientWorker = server.getClientWorker(key);
						if (clientWorker.isPresent()) {
							treatMessageFromLocalClient(key, clientWorker.get());
						}
					}
				}
			}
		}
	}

	/**
	 * treats the messages received from a neighbouring server. <br>
	 * When this method is called, it is already synchronised on the server object.
	 * 
	 * @param key        the selection key corresponding to the worker. By
	 *                   construction, it is not {@code null}.
	 * @param readWorker the worker to read the message from. By construction, it is
	 *                   not {@code null}.
	 */
	private void treatMessageFromNeighbouringServer(final SelectionKey key, final FullDuplexMsgWorker readWorker) {
		// message comes from another server
		try {
			ReadMessageStatus status;
			status = readWorker.readMessage();
			if (status == ReadMessageStatus.CHANNELCLOSED) {
				// remote end point has been closed
				readWorker.close();
				server.removeServerWorker(key);
				if (LOG_ON && COMM.isInfoEnabled()) {
					COMM.info(Log.computeServerLogMessage(server, "Closing a channel"));
				}
			}
			if (status == ReadMessageStatus.READDATACOMPLETED) {
				int messType = readWorker.getInType();
				Serializable msg = readWorker.getData().orElseThrow(() -> new IllegalStateException("no data"));
				if (LOG_ON && COMM.isDebugEnabled()) {
					COMM.debug(
							Log.computeServerLogMessage(server, "Message received of type " + messType + ", " + msg));
				}
				parsePathOfMsgToUpdateRoutingInformation((MsgContent) msg);
				if (messType < chat.common.Action.OFFSET_CLIENT_ALGORITHMS) {
					// message for server
					// only ServerToServerMsgContent can be point-to-point
					if (msg instanceof ServerToServerMsgContent) {
						ServerToServerMsgContent m = (ServerToServerMsgContent) msg;
						// a point-to-point message not addressed to this server
						if (m.getIntendedRecipient() != ServerToServerMsgContent.NO_INTENDED_RECIPIENT
								&& m.getIntendedRecipient() != server.identity()) {
							if (LOG_ON && COMM.isDebugEnabled()) {
								COMM.debug(Log.computeServerLogMessage(server,
										"forwarding point-to-point message to server " + m.getIntendedRecipient()));
							}
							server.sendToAServer(m.getIntendedRecipient(), messType, m.getSender(), m);
							return;
						}
					}
					// either a point-to-point message addressed to this server or not a
					// point-to-point message
					if (LOG_ON && COMM.isTraceEnabled()) {
						COMM.trace(Log.computeServerLogMessage(server,
								"Going to execute action" + " for message type #" + messType + " on content " + msg));
					}
					chat.server.algorithms.Algorithm.execute(server, messType, msg);
				} else {
					// client message to forward
					if (!(msg instanceof ChatMsgContent)) {
						COMM.trace(Log.computeServerLogMessage(server,
								"Should be a chat message, but is not (" + msg.getClass() + ")"));
					}
					server.forward(key, messType, (ChatMsgContent) msg);
				}
			}
		} catch (IOException e) {
			COMM.error(e.getLocalizedMessage());
		}
	}

	/**
	 * parses the path of the message content to update routing information. The
	 * path of the message is reversed in order to find at the beginning of the path
	 * the identity of the neighbour. Then, the path is parsed and for each remote
	 * server that is found in the path, the routing information is updated.
	 * Updating the path is as follows: either the remote server is not already
	 * "known" or the path to the remote server is shorter than what is already
	 * "known". <br>
	 * When this method is called, it is already synchronised on the server object.
	 * 
	 * @param msg the message to parse.
	 */
	private void parsePathOfMsgToUpdateRoutingInformation(final MsgContent msg) {
		int index = 0;
		List<Integer> reversePath = new ArrayList<>(msg.getPath());
		Collections.reverse(reversePath);
		for (Integer id : reversePath) {
			index++;
			if (id != this.server.identity()) {
				server.updateRoutingInformation(id, index, reversePath.get(0), server.getSelectionKeyOfCurrentMsg()
						.orElseThrow(() -> new IllegalStateException("no current selection key")));
			}
		}
	}

	/**
	 * treats the messages received from a local client. <br>
	 * When this method is called, it is already synchronised on the server object.
	 * 
	 * @param key        the selection key corresponding to the worker.
	 * @param readWorker the worker to read the message from.
	 */
	private void treatMessageFromLocalClient(final SelectionKey key, final FullDuplexMsgWorker readWorker) {
		try {
			ReadMessageStatus status;
			status = readWorker.readMessage();
			if (status == ReadMessageStatus.CHANNELCLOSED) {
				readWorker.close();
				server.removeClientWorker(key);
				if (LOG_ON && COMM.isInfoEnabled()) {
					COMM.info(Log.computeServerLogMessage(server, "Closing a channel"));
				}
			}
			if (status == ReadMessageStatus.READDATACOMPLETED) {
				int messType = readWorker.getInType();
				Serializable msg = readWorker.getData().orElseThrow(() -> new IllegalStateException("no data"));
				if (LOG_ON && COMM.isInfoEnabled()) {
					COMM.info(Log.computeServerLogMessage(server,
							"Message received " + msg + " " + msg.getClass().getName()));
				}
				if (!(msg instanceof ChatMsgContent)) {
					throw new IllegalStateException("only ChatMessageContent can be received from local client");
				}
				server.forward(key, messType, (ChatMsgContent) msg);
			}
		} catch (IOException e) {
			COMM.error(e.getStackTrace());
		}
	}
}
