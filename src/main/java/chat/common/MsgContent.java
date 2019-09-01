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
package chat.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This abstract class defines the the message contents used in the client or
 * the server.
 * 
 * @author Denis Conan
 */
public abstract class MsgContent implements Serializable {
	/**
	 * serialisation number.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * the identity of the sender.
	 */
	private final int sender;
	/**
	 * the path of servers that forward the message. This attribute is modified by
	 * the servers that forward the message. It is used by servers to learn the set
	 * of reachable servers in the system, plus the identity of the neighbouring
	 * server for the next hop to a remote server.
	 */
	private final List<Integer> path;

	/**
	 * constructs a message. <br>
	 * The path is copied into an {@code ArrayList} so that it can be
	 * complemented during routing.
	 * 
	 * @param sender the identity of the sender.
	 * @param path   the path of servers.
	 */
	public MsgContent(final int sender, final List<Integer> path) {
		if (sender < 0) {
			throw new IllegalArgumentException("invalid id for the sender (" + sender + ")");
		}
		Objects.requireNonNull("argument path cannot be null");
		this.path = new ArrayList<>(path);
		this.sender = sender;
		assert invariantMsgContent();
	}

	/**
	 * constructs a message with an empty path.
	 * 
	 * @param sender the identity of the sender.
	 */
	public MsgContent(final int sender) {
		this(sender, new ArrayList<>());
	}

	/**
	 * checks the invariant of the class.
	 * 
	 * @return the boolean stating the invariant is maintained.
	 */
	public final boolean invariantMsgContent() {
		return sender >= 0 && path != null;
	}

	/**
	 * gets the identity of the sender.
	 * 
	 * @return the identity of the sender.
	 */
	public int getSender() {
		return sender;
	}

	/**
	 * gets the path.
	 * 
	 * @return the path.
	 */
	public List<Integer> getPath() {
		return path;
	}

	/**
	 * appends the server's identity to the path in order to indicate that the
	 * message has been forwarded by the given server.
	 * 
	 * @param identity the identity of the new server.
	 */
	public void appendToPath(final int identity) {
		path.add(identity);
	}

	/**
	 * gets the string version of the path of the message.
	 * 
	 * @return the string.
	 */
	public String toStringPath() {
		return path.stream().map(Object::toString).collect(Collectors.joining(", "));
	}
}
