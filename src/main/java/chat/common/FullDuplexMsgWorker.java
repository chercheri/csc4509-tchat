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

Initial developer(s): Christian Bac, Denis Conan
Contributor(s):
 */
package chat.common;

import static chat.common.Log.COMM;
import static chat.common.Log.LOG_ON;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.Optional;

/**
 * This class defines a message as a set of byte buffers. <br>
 * A message is:
 * <ul>
 * <li>the message type as an int</li>
 * <li>the identity of the sender as an int</li>
 * <li>the sequence number of sender as an int</li>
 * <li>the message size as an int</li>
 * <li>the data as a serialised object</li>
 * </ul>
 * 
 * @author chris
 * @author Denis Conan
 * 
 */
public class FullDuplexMsgWorker {
	/**
	 * this arrays can contain message headers in the first buffer (fixed size) and
	 * message body which size is described in the header. We need two ByteBuffers
	 * due to asynchronism in input and output. Put and get operations are not done
	 * at once.
	 */
	private ByteBuffer[] inBuffers;
	/**
	 * this arrays can contain message headers in the first buffer (fixed size) and
	 * message body which size is described in the header. We need two ByteBuffers
	 * due to asynchronism in input and output. Put and get operations are not done
	 * at once.
	 */
	private ByteBuffer[] outBuffers;
	/**
	 * read message status, to describe completeness of data reception.
	 */
	private ReadMessageStatus readState;
	/**
	 * the socket channel.
	 */
	private SocketChannel rwChan = null;
	/**
	 * the type of the last message received.
	 */
	private int inType;
	/**
	 * the size of the last message received.
	 */
	private int inSize;
	/**
	 * the identity of the last message received.
	 */
	private int inIdentity;
	/**
	 * the size of headers.
	 */
	private static final int SIZE_HEADER = 3;

	/**
	 * is the public constructor for an open channel---i.e., after accept.
	 * 
	 * @param channel the socket channel that has been accepted.
	 */
	public FullDuplexMsgWorker(final SocketChannel channel) {
		Objects.requireNonNull(channel, "argument channel cannot be null");
		inBuffers = new ByteBuffer[2];
		outBuffers = new ByteBuffer[2];
		inBuffers[0] = ByteBuffer.allocate(Integer.SIZE * SIZE_HEADER / Byte.SIZE);
		outBuffers[0] = ByteBuffer.allocate(Integer.SIZE * SIZE_HEADER / Byte.SIZE);
		inBuffers[1] = null;
		outBuffers[1] = null;
		readState = ReadMessageStatus.READ_UNSTARTED;
		rwChan = channel;
		assert invariant();
	}

	/**
	 * checks the invariant of the class.
	 * 
	 * Be careful! this method must stay {@code private} in this class in order to
	 * avoid a redefinition in subclasses. If made {@code public} then do not call
	 * the method in the constructor of this class.
	 * 
	 * @return a boolean stating whether the invariant is maintained.
	 */
	private boolean invariant() {
		return inBuffers != null && inBuffers.length == 2 && outBuffers != null && outBuffers.length == 2
				&& (inBuffers[0].capacity() > 0) && (outBuffers[0].capacity() > 0) && rwChan != null;
	}

	/**
	 * configures the channel in non blocking mode.
	 * 
	 * @throws IOException the exception thrown in case of configuration problem.
	 */
	public void configureNonBlocking() throws IOException {
		rwChan.configureBlocking(false);
	}

	/**
	 * gets the current channel of this worker.
	 * 
	 * @return my channel
	 */
	public SocketChannel getChannel() {
		return rwChan;
	}

	/**
	 * sends a message using channel.
	 * 
	 * @param type     message type.
	 * @param identity the identity to be inserted in the message to send.
	 * @param s        the content of the message as a serialised object.
	 * @return size of the data send.
	 * @throws IOException the exception thrown in case of IO problem.
	 */
	public long sendMsg(final int type, final int identity, final Serializable s) throws IOException {
		Objects.requireNonNull(s, "argument s cannot be null");
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		int size;
		ObjectOutputStream oo = new ObjectOutputStream(bo);
		oo.writeObject(s);
		oo.close();
		size = bo.size();
		int outType = type;
		int outSize = size;
		int outIdentity = identity;
		outBuffers[0].clear();
		outBuffers[0].putInt(outType);
		outBuffers[0].putInt(outIdentity);
		outBuffers[0].putInt(outSize);
		outBuffers[0].flip();
		outBuffers[1] = ByteBuffer.allocate(size);
		outBuffers[1].put(bo.toByteArray());
		bo.close();
		outBuffers[1].flip();
		rwChan.write(outBuffers);
		assert invariant();
		return size;
	}

	/**
	 * closes the channel.
	 * 
	 * @throws IOException the exception thrown in case of problem.
	 */
	public void close() throws IOException {
		rwChan.close();
	}

	/**
	 * reads a message.
	 * 
	 * @return a ReadMessageStatus to specify read progress.
	 */
	public ReadMessageStatus readMessage() {
		int recvSize;
		if (readState == ReadMessageStatus.READ_UNSTARTED) {
			inBuffers[0].clear();
			readState = ReadMessageStatus.READHEADERSTARTED;
		}
		if (readState == ReadMessageStatus.READDATACOMPLETED) {
			inBuffers[0].clear();
			inBuffers[1] = null;
			readState = ReadMessageStatus.READHEADERSTARTED;
		}
		if (readState == ReadMessageStatus.READHEADERSTARTED) {
			if (inBuffers[0].position() < inBuffers[0].capacity()) {
				try {
					recvSize = rwChan.read(inBuffers[0]);
					if (LOG_ON && COMM.isTraceEnabled()) {
						COMM.trace("	Received       : " + recvSize);
					}
					if (recvSize == 0) {
						assert invariant();
						return readState;
					}
					if (recvSize < 0) {
						readState = ReadMessageStatus.CHANNELCLOSED;
						close();
						assert invariant();
						return readState;
					}
					if (inBuffers[0].position() < inBuffers[0].capacity()) {
						assert invariant();
						return readState;
					}
				} catch (IOException e) {
					if (Thread.interrupted()) {
						return ReadMessageStatus.CHANNELCLOSED;
					}
					COMM.error(e);
					readState = ReadMessageStatus.CHANNELCLOSED;
					try {
						if (LOG_ON && COMM.isTraceEnabled()) {
							COMM.trace("Closing a connection");
						}
						close();
					} catch (IOException closeException) {
						if (LOG_ON && COMM.isTraceEnabled()) {
							COMM.trace("problem when closing the connection");
						}
					}
					assert invariant();
					return readState;
				}
			}
			inBuffers[0].flip();
			if (LOG_ON && COMM.isTraceEnabled()) {
				COMM.trace("Position and limit : " + inBuffers[0].position() + " " + inBuffers[0].limit());
			}
			inType = inBuffers[0].getInt();
			inIdentity = inBuffers[0].getInt();
			inSize = inBuffers[0].getInt();
			if (LOG_ON && COMM.isTraceEnabled()) {
				COMM.trace("Message type and size : " + inType + " " + inSize);
			}
			inBuffers[0].rewind();
			readState = ReadMessageStatus.READHEADERCOMPLETED;
		}
		if (readState == ReadMessageStatus.READHEADERCOMPLETED) {
			if (inBuffers[1] == null || inBuffers[1].capacity() != inSize) {
				inBuffers[1] = ByteBuffer.allocate(inSize);
			}
			readState = ReadMessageStatus.READDATASTARTED;
		}
		if (readState == ReadMessageStatus.READDATASTARTED) {
			if (inBuffers[1].position() < inBuffers[1].capacity()) {
				try {
					recvSize = rwChan.read(inBuffers[1]);
					if (LOG_ON && COMM.isTraceEnabled()) {
						COMM.trace("	Received       : " + recvSize);
					}
					if (recvSize == 0) {
						assert invariant();
						return readState;
					}
					if (recvSize < 0) {
						close();
						readState = ReadMessageStatus.CHANNELCLOSED;
						assert invariant();
						return readState;
					}
				} catch (IOException e) {
					if (Thread.interrupted()) {
						return ReadMessageStatus.CHANNELCLOSED;
					}
					COMM.error(e);
					readState = ReadMessageStatus.CHANNELCLOSED;
					try {
						if (LOG_ON && COMM.isTraceEnabled()) {
							COMM.trace("Closing a connection");
						}
						close();
					} catch (IOException closeException) {
						if (LOG_ON && COMM.isTraceEnabled()) {
							COMM.trace("problem when closing the connection");
						}
					}
					return readState;
				}
			}
			if (LOG_ON && COMM.isTraceEnabled()) {
				COMM.trace("Position and capacity : " + inBuffers[1].position() + " " + inBuffers[1].capacity());
			}
			if (inBuffers[1].position() == inBuffers[1].capacity()) {
				readState = ReadMessageStatus.READDATACOMPLETED;
			}
		}
		return readState;
	}

	/**
	 * returns the Serializable data build out of the data part of the received
	 * message when the readState is ReadDataCompleted. This operation should be
	 * stateless for the ByteBuffers, meaning that we can getData and after write
	 * the ByteBuffer if necessary.
	 * 
	 * @return unserialised data.
	 * @throws IOException the exception thrown in case of problem.
	 */
	public Optional<Serializable> getData() throws IOException {
		Serializable res = null;
		if (readState == ReadMessageStatus.READDATACOMPLETED) {
			try {
				inBuffers[1].flip();
				byte[] cpBb = new byte[inBuffers[1].limit()];
				inBuffers[1].get(cpBb);
				ByteArrayInputStream bi = new ByteArrayInputStream(inBuffers[1].array());
				ObjectInputStream oi = new ObjectInputStream(bi);
				res = (Serializable) oi.readObject();
				oi.close();
				bi.close();
			} catch (ClassNotFoundException e) {
				COMM.error(e);
				throw new RuntimeException("pb in FullDuplexMsgWorker::getData (" + e.getLocalizedMessage() + ")");
			}
		}
		inBuffers[1].rewind();
		assert invariant();
		return Optional.ofNullable(res);
	}

	/**
	 * gets the type (an integer) of the last message received.
	 * 
	 * @return the type of the last message received.
	 */
	public int getInType() {
		return inType;
	}

	/**
	 * gets the identity (an integer) of the last message received.
	 * 
	 * @return the identity of the last message received.
	 */
	public int getInIdentity() {
		return inIdentity;
	}
}
