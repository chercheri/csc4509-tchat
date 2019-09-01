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
import static chat.common.Log.ELECTION;
import static chat.common.Log.MUTEX;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import chat.server.algorithms.Algorithm;
import chat.client.algorithms.chat.ChatMsgContent;
import chat.common.Entity;
import chat.common.FullDuplexMsgWorker;
import chat.common.Log;
import chat.common.MsgContent;
import chat.common.Scenario;
import chat.common.VectorClock;
import chat.server.algorithms.election.ElectionAction;
import chat.server.algorithms.election.ElectionLeaderContent;
import chat.server.algorithms.election.ElectionTokenContent;
import chat.server.algorithms.mutex.MutexAction;
import chat.server.algorithms.mutex.MutexRequestContent;
import chat.server.algorithms.mutex.MutexStatus;
import chat.server.algorithms.mutex.MutexTokenContent;

/**
 * This class defines server object. The server object connects to existing chat
 * servers, waits for connections from other chat servers and from chat clients,
 * and forwards chat messages received from chat clients to other 'local' chat
 * clients and to the other chat servers.
 * 
 * The chat servers can be organised into a network topology forming cycles
 * since the method <tt>forward</tt> is only called when the message to forward
 * has not already been received and forwarded.
 * 
 * This class is provided as the starting point to implement distributed
 * algorithms. This explains why it is abstract.
 * 
 * @author Denis Conan
 * 
 */
public class Server implements Entity {

	/**
	 * je sais pas encore.
	 */
	private int ns;
	/**
	 * vecteur demande[].
	 */
	private VectorClock request;
	/**
	 * vecteur jeton[].
	 */
	private  MutexTokenContent token;


	/**
	 * boolean
	 */
	private static boolean saveInFile = false;


	/**
	 * presence du jeton.
	 */
	private boolean hasToken;
	/**
	 * state of the server (DANS_CS, HORS_CS, EN_ATT).
	 */
	private MutexStatus mutexState; 
	/**
	 * the caw ; l'identifiant de l'initiateur .
	 */
	private int caw;
	/**
	 * the parent.
	 */
	private int parent;
	/**
	 * the win : ce gagnant . 
	 */
	private int win;
	/**
	 * the rec= le nbr de jetons recu.
	 */
	private int rec;
	/**
	 * the lrec= le nbr de msg gagnants recus.
	 */
	private int lrec;
	/**
	 * the status: le statut du serveur.
	 */
	private String status;
	/**
	 * the base of the port number for connecting to clients.
	 */
	private static final int BASE_PORTNB_LISTEN_CLIENT = 2050;
	/**
	 * the offset of the port number for connecting to servers.
	 */
	private static final int OFFSET_PORTNB_LISTEN_SERVER = 100;
	/**
	 * the number of clients that have opened a connection to this server till the
	 * beginning of its execution. Each client is assigned an identity in the form
	 * of an integer and this identity is provided by the server it is connected to:
	 * it is the current value of this integer.
	 */
	private int numberOfClientsSinceBeginning = 0;
	/**
	 * the selector.
	 */
	private final Selector selector;
	/**
	 * the runnable object of the server that receives the messages from the chat
	 * clients and the other chat servers.
	 */
	private final ReadMessagesFromNetwork runnableToRcvMsgs;
	/**
	 * the thread of the server that receives the messages from the chat clients and
	 * the other chat servers.
	 */
	private final Thread threadToRcvMsgs;
	/**
	 * identity of this server.
	 */
	private final int identity;
	/**
	 * selection key of the connection from which the last message was received.
	 */
	private SelectionKey selectionKeyOfCurrentMsg;
	/**
	 * collection of selection keys of the server message workers.
	 */
	private final Map<SelectionKey, FullDuplexMsgWorker> allServerWorkers;
	/**
	 * collection of selection keys with the identity of the neighbouring server.
	 */
	private final Map<SelectionKey, Integer> neighbouringServerIdentities;
	/**
	 * collection of reachable brokers, that is the already collected routing
	 * information data structures. The key of the identity of the remote broker.
	 * <br>
	 * ASSUMPTION: no topology changes, except in the initialisation phase or in the
	 * termination phase.
	 */
	private final Map<Integer, RoutingInformation> reachableServers;
	/**
	 * the offset to compute the identity of the new client has a function of the
	 * identity of the server and the number of connected clients.
	 */
	public static final int OFFSET_ID_CLIENT = 100;
	/**
	 * selection keys of the client message workers.
	 */
	private final Map<SelectionKey, FullDuplexMsgWorker> allClientWorkers;
	/**
	 * since there may exist cycles in the topology of servers, chat messages can go
	 * through several path from the sender to this server, and hence a client
	 * message may receive the same chat message several times. This collection
	 * stores the last chat message received from each client in order to forward a
	 * chat message once to local clients. The key of the map is the client
	 * identifier and the value is the greatest sequence number.<br>
	 * It works till there is no loss (an assumption in our study) and messages are
	 * not reordered (an assumption of TCP network links).
	 */
	private final Map<Integer, Integer> sequenceNumberOfLocalClients;

	/**
	 * initialises the collection attributes and the state of the server, and
	 * creates the channels that are accepting connections from clients and servers.
	 * At the end of the constructor, the server opens connections to the other
	 * servers (hostname, identifier) that are provided in the command line
	 * arguments.
	 * 
	 * NB: after the construction of a client object, the thread for reading
	 * messages must be started using the method
	 * {@link #startThreadReadMessagesFromNetwork}.
	 * 
	 * @param args java command arguments.
	 */
	public Server(final String[] args) {
		Objects.requireNonNull(args, "args cannot be null");
		identity = Integer.parseInt(args[0]);
		int portnum = BASE_PORTNB_LISTEN_CLIENT + Integer.parseInt(args[0]);
		allServerWorkers = new HashMap<>();
		neighbouringServerIdentities = new HashMap<>();
		reachableServers = new HashMap<>();
		allClientWorkers = new HashMap<>();
		sequenceNumberOfLocalClients = new HashMap<>();
		InetSocketAddress rcvAddressClient;
		InetSocketAddress rcvAddressServer;
		try {
			selector = Selector.open();
		} catch (IOException e) {
			throw new IllegalStateException("cannot create the selector");
		}
		ServerSocketChannel listenChanClient = null;
		ServerSocketChannel listenChanServer = null;
		try {
			listenChanClient = ServerSocketChannel.open();
			listenChanClient.configureBlocking(false);
		} catch (IOException e) {
			throw new IllegalStateException("cannot set the blocking option to a server socket");
		}
		try {
			listenChanServer = ServerSocketChannel.open();
		} catch (IOException e) {
			throw new IllegalStateException("cannot open the server socket" + " for accepting server connections");
		}
		try {
			rcvAddressClient = new InetSocketAddress(portnum);
			listenChanClient.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			rcvAddressServer = new InetSocketAddress(portnum + OFFSET_PORTNB_LISTEN_SERVER);
			listenChanServer.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		} catch (IOException e) {
			throw new IllegalStateException("cannot set the SO_REUSEADDR option");
		}
		try {
			listenChanClient.bind(rcvAddressClient);
			listenChanServer.bind(rcvAddressServer);
		} catch (IOException e) {
			throw new IllegalStateException("cannot bind to a server socket");
		}
		try {
			listenChanClient.configureBlocking(false);
			listenChanServer.configureBlocking(false);
		} catch (IOException e) {
			throw new IllegalStateException("cannot set the blocking option");
		}
		SelectionKey acceptClientKey = null;
		SelectionKey acceptServerKey = null;
		try {
			acceptClientKey = listenChanClient.register(selector, SelectionKey.OP_ACCEPT);
			acceptServerKey = listenChanServer.register(selector, SelectionKey.OP_ACCEPT);
		} catch (ClosedChannelException e) {
			throw new IllegalStateException("cannot register a server socket");
		}
		if (LOG_ON && COMM.isInfoEnabled()) {
			COMM.info(Log.computeServerLogMessage(this,
					"  listenChanClient ok on port " + listenChanClient.socket().getLocalPort()));
			COMM.info(Log.computeServerLogMessage(this,
					"  listenChanServer ok on port " + listenChanServer.socket().getLocalPort()));
		}
		runnableToRcvMsgs = new ReadMessagesFromNetwork(this, selector, acceptClientKey, listenChanClient,
				acceptServerKey, listenChanServer);
		threadToRcvMsgs = new Thread(runnableToRcvMsgs);
		for (int i = 1; i < args.length; i = i + 2) {
			try {
				addServer(args[i],
						(BASE_PORTNB_LISTEN_CLIENT + Integer.parseInt(args[i + 1]) + OFFSET_PORTNB_LISTEN_SERVER));
			} catch (IOException e) {
				COMM.error(e.getLocalizedMessage());
				return;
			}
		}
		/* Initalisation des variables pour l'algorithme de l'election */
		this.parent = -1;
		this.caw = -1;
		this.win = -1;
		this.rec = 0;
		this.lrec = 0;
		this.status = "dormant";

		/* Initialisation des variables pour l'algorithme de l'exclusion mutuelle*/
		this.mutexState = MutexStatus.HORS_CS;
		this.ns = 0;
		this.request = new VectorClock();

		this.hasToken = false;



		assert invariant();
	}

	/**
	 * connects socket, creates MsgWorker, and registers selection key of the remote
	 * server. This method is called when connecting to a remote server. Connection
	 * data are provided as arguments to the main.
	 * 
	 * @param host remote host's name.
	 * @param port remote port's number.
	 * @throws IOException the exception thrown in case of communication problem.
	 */
	private void addServer(final String host, final int port) throws IOException {
		Objects.requireNonNull(host, "argument host cannot be null");
		Socket rwSock;
		SocketChannel rwChan;
		InetSocketAddress rcvAddress;
		if (LOG_ON && COMM.isInfoEnabled()) {
			COMM.info(Log.computeServerLogMessage(this,
					"opening connection with server on host " + host + " on port " + port));
		}
		InetAddress destAddr = InetAddress.getByName(host);
		rwChan = SocketChannel.open();
		rwSock = rwChan.socket();
		// obtain the IP address of the target host
		rcvAddress = new InetSocketAddress(destAddr, port);
		// connect sending socket to remote port
		rwSock.connect(rcvAddress);
		FullDuplexMsgWorker worker = new FullDuplexMsgWorker(rwChan);
		worker.configureNonBlocking();
		SelectionKey serverKey = rwChan.register(selector, SelectionKey.OP_READ);
		synchronized (this) {
			allServerWorkers.put(serverKey, worker);
			if (LOG_ON && COMM.isDebugEnabled()) {
				COMM.debug(Log.computeServerLogMessage(this, "getAllServerWorkersSize() = " + allServerWorkers.size()));
			}
		}
		assert invariant();
	}

	/**
	 * add or update the routing information to the server
	 * {@code identityOfRemoteServer}. The information is updated only if server
	 * {@code identityOfRemoteServer} is unknown to this server or the length of the
	 * path is less than the existing information. <br>
	 * In addition, while the identities of all the neighbouring servers are not
	 * known, try filling {@link #neighbouringServerIdentities}.
	 * 
	 * @param identityOfRemoteServer         the identity of the remote server.
	 * @param pathLength                     the length of the path to the remote
	 *                                       server.
	 * @param identityNeighbouringServer     the identity to the neighbouring server
	 *                                       that can forward messages to the remote
	 *                                       server.
	 * @param selectionKeyNeighbouringServer the selection key to the neighbouring
	 *                                       server.
	 */
	synchronized void updateRoutingInformation(final int identityOfRemoteServer, final int pathLength,
			final int identityNeighbouringServer, final SelectionKey selectionKeyNeighbouringServer) {
		if (!reachableServers.containsKey(identityOfRemoteServer)
				|| (reachableServers.containsKey(identityOfRemoteServer)
						&& reachableServers.get(identityOfRemoteServer).getLengthOfThePath() > pathLength)) {
			reachableServers.put(identityOfRemoteServer, new RoutingInformation(identityOfRemoteServer, pathLength,
					identityNeighbouringServer, selectionKeyNeighbouringServer));
			if (LOG_ON && COMM.isDebugEnabled()) {
				COMM.debug(Log.computeServerLogMessage(this, "identityOfRemoteServer=" + identityOfRemoteServer
						+ ", pathLength=" + pathLength + ", identityNeighbouringServer=" + identityNeighbouringServer));
			}
			if (neighbouringServerIdentities.size() < allServerWorkers.size()
					&& !neighbouringServerIdentities.containsKey(selectionKeyNeighbouringServer)) {
				neighbouringServerIdentities.put(selectionKeyNeighbouringServer, identityNeighbouringServer);
			}
		}
		assert invariant();
	}

	@Override
	public synchronized int identity() {
		return identity;
	}

	/**
	 * gets the key of the SelectionKey that receives the current message.
	 * 
	 * @return the current selection key.
	 */
	synchronized Optional<SelectionKey> getSelectionKeyOfCurrentMsg() {
		return Optional.ofNullable(selectionKeyOfCurrentMsg);
	}

	/**
	 * sets the key of the SelectionKey that receives the current message. This
	 * selection key can be {@code null}.
	 * 
	 * @param selectionKeyOfCurrentMsg the current key.
	 */
	synchronized void setSelectionKeyOfCurrentMsg(final SelectionKey selectionKeyOfCurrentMsg) {
		this.selectionKeyOfCurrentMsg = selectionKeyOfCurrentMsg;
		assert invariant();
	}

	/**
	 * gets the worker to communicate with a given neighbouring servers.
	 * 
	 * @param key the selection key to get the worker.
	 * @return the given server worker.
	 */
	synchronized Optional<FullDuplexMsgWorker> getServerWorker(final SelectionKey key) {
		return Optional.ofNullable(allServerWorkers.get(key));
	}

	/**
	 * gets the identity of the neighbouring server that corresponds to the first
	 * hop of the path to a remote server.
	 * 
	 * @param identityOfReachableServer the identity of the remote server.
	 * @return the identity of the first server.
	 */
	public synchronized int getFirstHopToRemoteServer(final int identityOfReachableServer) {
		if (reachableServers.get(identityOfReachableServer) != null) {
			return reachableServers.get(identityOfReachableServer).getIdentityNeighbouringServer();
		} else {
			return -1;
		}
	}

	/**
	 * remove a worker to a given neighbouring server.
	 * 
	 * @param key the SelectionKey of the server worker to remove.
	 */
	synchronized void removeServerWorker(final SelectionKey key) {
		allServerWorkers.remove(key);
		assert invariant();
	}

	/**
	 * gets the worker to communicate with an attached client.
	 * 
	 * @param key the selection key to get the worker.
	 * @return the given client worker.
	 */
	synchronized Optional<FullDuplexMsgWorker> getClientWorker(final SelectionKey key) {
		return Optional.ofNullable(allClientWorkers.get(key));
	}

	/**
	 * remove a worker to a local client.
	 * 
	 * @param key the SelectionKey of the client worker to remove.
	 */
	synchronized void removeClientWorker(final SelectionKey key) {
		allClientWorkers.remove(key);
		assert invariant();
	}

	/**
	 * checks the invariant of the class.
	 * 
	 * @return a boolean stating whether the invariant is maintained.
	 */
	private synchronized boolean invariant() {
		return identity >= 0 && numberOfClientsSinceBeginning >= 0 && selector != null && runnableToRcvMsgs != null
				&& threadToRcvMsgs != null && allServerWorkers != null && neighbouringServerIdentities != null
				&& allClientWorkers != null;
	}

	/**
	 * starts the thread that is responible for reading messages from the clients
	 * and the other servers.
	 */
	public void startThreadReadMessagesFromNetwork() {
		threadToRcvMsgs.start();
	}

	/**
	 * treats an input line from the console. <br>
	 * Do not forget to synchronise with {@code synchronized}.
	 * 
	 * @param line the content of the message
	 */
	public void treatConsoleInput(final String line) {
		Objects.requireNonNull(line, "argument line cannot be null");
		if (LOG_ON && GEN.isDebugEnabled()) {
			GEN.debug(Log.computeServerLogMessage(this, "new command line on console"));
		}
		if (line.equals("quit")) {
			threadToRcvMsgs.interrupt();
			// do not interrupt the main thread during the execution of a Scenario because
			// all the clients and all the servers are controlled by the same "main" thread
			if (!Scenario.isJUnitScenario()) {
				Thread.currentThread().interrupt();
			}
		} else if (line.equals("start election") || line.equals("candidate")) {

			COMM.info("election start new candidate");
			if (this.allServerWorkers.size() == 0) {
				this.status = "gagant";
				COMM.info("Je suis: " + this.status);
			} else if (this.win == -1) {
				synchronized (this) {
					this.status = "initiator";
					this.caw = this.identity;
					ElectionTokenContent token = new ElectionTokenContent(identity, identity);
					this.sendToAllServers(Algorithm.getActionNumber(ElectionAction.TOKEN_MESSAGE), 0, token);
					COMM.info("mon status est = " + this.status);
				}
			}

		} else if (line.equals("demande jeton")) {
			MUTEX.info("Demande de Jeton");
			if (!this.hasToken) {
				this.ns++;
				for (RoutingInformation info : this.reachableServers.values()) {
					MUTEX.info("sending token request from " + this.identity + " to server" + info.getIndentityOfRemoteServer());
					this.sendToAServer(info.getIndentityOfRemoteServer(), Algorithm.getActionNumber(MutexAction.DEMANDE_MSG), identity, new MutexRequestContent(this.identity, this.ns, info.getIndentityOfRemoteServer()));
				}
			}
			
		} else if (line.equals("relacher jeton")) {
			if(!this.hasToken) {
				MUTEX.info(this.identity + " ne possède pas le jeton.");
				return;
			}
			
			MUTEX.info("Relachement du jeton... (Serveur " + this.identity + ")");
			token.getTokenVector().setEntry(this.identity, this.ns);
			int nbServer = this.reachableServers.size();
			for (int i = (this.identity + 1) % (nbServer+1); i != this.identity; i = (i + 1) % (nbServer+1)) {
				if (this.request.getEntry(i) > token.getTokenVector().getEntry(i) && this.hasToken) {
					token = new MutexTokenContent(this.identity, token.getTokenVector(), i);
					this.sendToAServer(i, Algorithm.getActionNumber(MutexAction.JETON_MSG), this.identity, token);
					this.hasToken = false;
					MUTEX.info("Token was sent from " + this.identity + " to " + i);
					return;
				}
			}
		}
		assert invariant();
	}

	/**
	 * accepts connection (socket level), creates MsgWorker, and registers selection
	 * key of the remote server. This method is called when accepting a connection
	 * from a remote server.
	 * 
	 * @param sc server socket channel.
	 * @throws IOException the exception thrown in case of communication problem.
	 */
	synchronized void acceptNewServer(final ServerSocketChannel sc) throws IOException {
		SocketChannel rwChan = sc.accept();
		if (rwChan != null) {
			try {
				FullDuplexMsgWorker worker = new FullDuplexMsgWorker(rwChan);
				worker.configureNonBlocking();
				synchronized (this) {
					SelectionKey newKey = rwChan.register(selector, SelectionKey.OP_READ);
					allServerWorkers.put(newKey, worker);
					if (LOG_ON && COMM.isDebugEnabled()) {
						COMM.debug(Log.computeServerLogMessage(this,
								"getAllServerWorkersSize() = " + allServerWorkers.size()));
					}
					assert invariant();
				}
			} catch (ClosedChannelException e) {
				COMM.error(e.getLocalizedMessage());
			}
		}
	}

	/**
	 * accepts connection (socket level), creates MsgWorker, and registers selection
	 * key of the local client. This method is called when accepting a connection
	 * from a local client.
	 * 
	 * @param sc server socket channel.
	 */
	synchronized void acceptNewClient(final ServerSocketChannel sc) {
		SocketChannel rwChan = null;
		try {
			rwChan = sc.accept();
		} catch (IOException e) {
			COMM.error(Log.computeServerLogMessage(this, e.getLocalizedMessage()));
			return;
		}
		if (LOG_ON && COMM.isDebugEnabled()) {
			COMM.debug(Log.computeServerLogMessage(this, ", accepting a client connection"));
		}
		if (rwChan != null) {
			try {
				FullDuplexMsgWorker worker = new FullDuplexMsgWorker(rwChan);
				worker.configureNonBlocking();
				SelectionKey newKey = rwChan.register(selector, SelectionKey.OP_READ);
				worker.sendMsg(0, identity,
						Integer.valueOf(identity * OFFSET_ID_CLIENT + numberOfClientsSinceBeginning));
				allClientWorkers.put(newKey, worker);
				numberOfClientsSinceBeginning++;
				if (LOG_ON && COMM.isDebugEnabled()) {
					COMM.debug(Log.computeServerLogMessage(this,
							"getAllClientWorkersSize() = " + allClientWorkers.size()));
				}
				assert invariant();
			} catch (IOException e) {
				COMM.error(Log.computeServerLogMessage(this, e.getLocalizedMessage()));
			}
		}
	}

	/**
	 * sends a message to a particular server using the selection key of the
	 * neighbouring server from which that server is reachable. The method uses the
	 * routing information objects {@link RoutingInformation}. This is a utility
	 * method for implementing distributed algorithms in the servers' state machine:
	 * use this method when this server needs sending messages to a given remote
	 * server using the collection {@link #reachableServers}.
	 * 
	 * @param remoteServerIdentity the identity of the remote server.
	 * @param type                 message's type.
	 * @param identity             sender's identity, that is the identity of this
	 *                             server.
	 * @param msg                  message as a serializable object.
	 */
	public synchronized void sendToAServer(final int remoteServerIdentity, final int type, final int identity,
			final MsgContent msg) {
		if (remoteServerIdentity == identity()) {
			throw new UnsupportedOperationException("sending to \"myself\" ist not supported");
		}
		RoutingInformation ri = reachableServers.get(remoteServerIdentity);
		Objects.requireNonNull(ri == null);
		Optional<FullDuplexMsgWorker> sendWorker = getServerWorker(ri.getSelectionKeyOfNeighbouringServer());
		if (!sendWorker.isPresent()) {
			COMM.error(Log.computeServerLogMessage(this, "tries to send a message, but null SelectionKey"));
			throw new IllegalStateException("pb in Server::sendToAServer (unknown send worker)");
		} else {
			if (!msg.getPath().contains(identity())) {
				msg.appendToPath(identity());
			}
			if (LOG_ON && COMM.isInfoEnabled()) {
				COMM.info(Log.computeServerLogMessage(this,
						"sends message of type " + type + " to server " + remoteServerIdentity));
			}
			try {
				sendWorker.get().sendMsg(type, identity(), msg);
			} catch (IOException e) {
				removeServerWorker(ri.getSelectionKeyOfNeighbouringServer());
				COMM.error(Log.computeServerLogMessage(this, e.getLocalizedMessage()));
			}
		}
		assert invariant();
	}

	/**
	 * sends a message to a particular neighbour using its selection key.
	 * 
	 * @param targetKey selection key of the neighbour.
	 * @param type      message's type.
	 * @param msg       message as a serializable object.
	 */
	private synchronized void sendToAServer(final SelectionKey targetKey, final int type, final MsgContent msg) {
		Optional<FullDuplexMsgWorker> sendWorker = getServerWorker(targetKey);
		if (!sendWorker.isPresent()) {
			COMM.error(Log.computeServerLogMessage(this, "tries to send a message, but null SelectionKey"));
			throw new IllegalStateException("pb in Server::sendToAServer (unknown send worker)");
		} else {
			if (!msg.getPath().contains(identity())) {
				msg.appendToPath(identity());
			}
			if (LOG_ON && COMM.isInfoEnabled()) {
				COMM.info(Log.computeServerLogMessage(this, "sends message of type " + type));
			}
			try {
				sendWorker.get().sendMsg(type, identity(), msg);
			} catch (IOException e) {
				removeServerWorker(targetKey);
				COMM.error(Log.computeServerLogMessage(this, e.getLocalizedMessage()));
			}
		}
		assert invariant();
	}

	/**
	 * sends a message to all the remote servers / neighbours connected to this
	 * server. This is a utility method for implementing distributed algorithms in
	 * the servers' state machine: use this method when this server needs sending
	 * messages to its neighbours.
	 * 
	 * @param type message's type.
	 * @param seqN message's sequence number.
	 * @param msg  message as a serializable object.
	 */
	public synchronized void sendToAllServers(final int type, final int seqN, final MsgContent msg) {
		// send to all the servers, thus first argument is null
		forwardServers(null, type, msg);
		assert invariant();
	}

	/**
	 * sends a message to all the remote servers / neighbours connected to this
	 * server, except one. This is a utility method for implementing distributed
	 * algorithms in the servers' state machine: use this method when this server
	 * needs sending messages to all its neighbours, except one.
	 * 
	 * @param exceptKey the selection key of the server to exclude in the
	 *                  forwarding.
	 * @param type      message's type.
	 * @param msg       message as a serializable object.
	 */
	public synchronized void sendToAllServersExceptOne(final SelectionKey exceptKey, final int type,
			final MsgContent msg) {
		forwardServers(exceptKey, type, msg);
		assert invariant();
	}

	/**
	 * forwards a message to all the clients and the servers, except the entity
	 * (client or server) from which the message has just been received.
	 * 
	 * @param exceptKey selection key to exclude from the set of target connections,
	 *                  e.g., selection key of the entity from which the message has
	 *                  been received.
	 * @param type      message's type.
	 * @param msg       message as a {@link MsgContent} object.
	 * @throws IOException the communication exception thrown when sending the
	 *                     message.
	 */
	synchronized void forward(final SelectionKey exceptKey, final int type, final MsgContent msg) throws IOException {
		forwardServers(exceptKey, type, msg);
		if (msg instanceof ChatMsgContent) {
			forwardClients(exceptKey, type, (ChatMsgContent) msg);

		}
		assert invariant();
	}

	/**
	 * forwards a message to all the servers, except the server from which the
	 * message has just been received.
	 * 
	 * @param exceptKey selection key to exclude from the set of target connections,
	 *                  e.g., selection key of the entity from which the message has
	 *                  been received.
	 * @param type      message's type.
	 * @param msg       message as a {@link MsgContent} object.
	 */
	private synchronized void forwardServers(final SelectionKey exceptKey, final int type, final MsgContent msg) {
		int nbServers = 0;
		for (Map.Entry<SelectionKey, FullDuplexMsgWorker> entry : allServerWorkers.entrySet()) {
			if (entry.getKey() == exceptKey) {
				if (LOG_ON && COMM.isDebugEnabled()) {
					COMM.debug(Log.computeServerLogMessage(this,
							"does not send to a server " + "because (target == exceptKey)"));
				}
			} else {
				if (entry.getValue() == null) {
					COMM.error("Bad worker for server key " + entry.getKey());
					throw new IllegalStateException("pb in Server::forwardServers (unknown server selection key)");
				} else {
					if (!msg.getPath().contains(neighbouringServerIdentities.getOrDefault(entry.getKey(), -1))) {
						sendToAServer(entry.getKey(), type, msg);
						nbServers++;
					}
				}
			}
		}
		if (LOG_ON && COMM.isInfoEnabled()) {
			COMM.info(Log.computeServerLogMessage(this, "sends message to " + nbServers + " server end points"));
		}
		assert invariant();
	}

	/**
	 * forwards a message to all the clients, except the client from which the
	 * message has just been received.
	 * 
	 * @param exceptKey selection key to exclude from the set of target connections,
	 *                  e.g., selection key of the entity from which the message has
	 *                  been received.
	 * @param type      message's type.
	 * @param msg       message as a {@link ChatMsgContent} object.
	 */
	private synchronized void forwardClients(final SelectionKey exceptKey, final int type, final ChatMsgContent msg) {
		int nbClients = 0;
		if (msg.getSequenceNumber() > sequenceNumberOfLocalClients.getOrDefault(msg.getSender(), -1)) {
			for (Map.Entry<SelectionKey, FullDuplexMsgWorker> entry : allClientWorkers.entrySet()) {
				if (entry.getKey() == exceptKey) {
					if (LOG_ON && COMM.isDebugEnabled()) {
						COMM.debug(Log.computeServerLogMessage(this,
								"does not send to a client " + "because (target == exceptKey)"));
					}
				} else {
					if (entry.getValue() == null) {
						COMM.error("Bad client for key " + entry);
						throw new IllegalStateException("pb in Server::forwardServers (unknown client selection key)");
					} else {
						if (LOG_ON && COMM.isInfoEnabled()) {
							COMM.info(Log.computeServerLogMessage(this,
									"sends message to " + nbClients + " client end points"));
						}
						try {
							entry.getValue().sendMsg(type, identity(), msg);
						} catch (IOException e) {
							removeClientWorker(entry.getKey());
							COMM.error(Log.computeServerLogMessage(this, e.getLocalizedMessage()));
							return;
						}
						nbClients++;
						sequenceNumberOfLocalClients.put(msg.getSender(), msg.getSequenceNumber());

					}
				}
			}
		} else {
			if (LOG_ON && COMM.isTraceEnabled()) {
				COMM.trace(Log.computeServerLogMessage(this, "duplicate message (" + msg + ")"));
			}
		}

		assert invariant();
	}

	/**
	 * treats a token message of the election algorithm. <br>
	 * The actions of an algorithm are atomic. So, this method is synchronised.
	 * 
	 * @param content the content of the message to treat.
	 */
	public synchronized void receiveTokenContent(final ElectionTokenContent content) {


		// TODO to write. Please remove this comment when the method is implemented!

		if (this.caw == -1 || content.getInitiator() < this.caw) {
			this.caw = content.getInitiator();
			this.rec = 0;
			this.parent = content.getSender();

			ElectionTokenContent token = new ElectionTokenContent(this.identity, content.getInitiator());

			this.sendToAllServersExceptOne(this.reachableServers.get(this.parent).getSelectionKeyOfNeighbouringServer(),
					Algorithm.getActionNumber(ElectionAction.TOKEN_MESSAGE), token);
		}


		if (this.caw == content.getInitiator()) {
			this.rec++;


			if (rec == allServerWorkers.size()) {
				if (this.caw == this.identity) {
					ElectionLeaderContent leader = new ElectionLeaderContent(this.identity, this.identity);
					this.sendToAllServers(Algorithm.getActionNumber(ElectionAction.LEADER_MESSAGE), 0, leader);
				} else {
					ElectionTokenContent token = new ElectionTokenContent(this.identity, content.getInitiator());
					this.sendToAServer(this.parent, Algorithm.getActionNumber(ElectionAction.TOKEN_MESSAGE), 
							identity, token);
				}
			}
		}

		ELECTION.info("Jeton reçu : ");
		ELECTION.info("id du serveur = " + this.identity);
		ELECTION.info("caw = " + this.caw);
		ELECTION.info("parent =" + this.parent);
		ELECTION.info("nbr jeton reçu(rec) = " + this.rec);


		setSelectionKeyOfCurrentMsg(null);
		assert invariant();
	}

	/**
	 * treats a leader message of the election algorithm.<br>
	 * The actions of an algorithm are atomic. So, this method is synchronised.
	 * 
	 * @param content the content of the message to treat.
	 */
	public synchronized void receiveLeaderContent(final ElectionLeaderContent content) {
		// TODO to write. Please remove this comment when the method is implemented!

		if (lrec == 0 && this.identity != content.getInitiator()) {
			ElectionLeaderContent leader = new ElectionLeaderContent(this.identity, content.getInitiator());
			this.sendToAllServers(Algorithm.getActionNumber(ElectionAction.LEADER_MESSAGE), 0, leader);
		}

		lrec++;
		this.win = content.getInitiator();

		if (this.lrec == allServerWorkers.size()) {
			if (this.win == this.identity) {
				this.status = "leader";
				this.hasToken = true;
				System.out.println("here");
				token = new MutexTokenContent(this.identity, new VectorClock());
			} else {
				this.status = "non-leader";
			}
		}

		ELECTION.info("Gagant reçu : ");
		ELECTION.info("id du serveur = " + this.identity);
		ELECTION.info("caw = " + this.caw);
		ELECTION.info("parent =" + this.parent);
		ELECTION.info("nbr leader reçu(lrec) = " + this.lrec);
		ELECTION.info("win = " + this.win);


		setSelectionKeyOfCurrentMsg(null);
		assert invariant();
	}
	/**
	 *  
	 * @return
	 * 	 the identity of the leader.
	 */
	public int getLeader() {
		return this.win;
	}



	/**
	 * @param requestq
	 * 	 the content of the message to treat.
	 */
	public synchronized void receiveMutexRequest(final MutexRequestContent requestq) {
		
		MUTEX.info("server " + this.identity+ " received request from server " + requestq.getSender());
		if(this.hasToken) {
			MUTEX.info("server " + this.identity + " has token.");
		}
		this.request.setEntry(requestq.getSender(), Math.max(requestq.getNs(), this.request.getEntry(requestq.getSender())));

	}

	/**
	 * @param token
	 * 	 the content of the message to treat.
	 */
	public synchronized void receiveMutexToken(final MutexTokenContent token) {
		this.token = new MutexTokenContent(token);
		MUTEX.info("server " + this.identity + " received token");
		this.hasToken = true;
	}

	/**
	 * 
	 */
	public boolean getHasToken() {
		return hasToken;
	}


}
