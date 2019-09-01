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
package chat.server.algorithms;

import java.util.ArrayList;
import java.util.List;

import chat.common.MsgContent;

/**
 * This abstract class defines the the message contents used by servers (only)
 * to exchange information. <br>
 * The topology is not a full-meshed topology, and consequently, an attribute is
 * added to allow point-to-point communication between servers.
 * 
 * @author Denis Conan
 */
public abstract class ServerToServerMsgContent extends MsgContent {
	/**
	 * serialisation number.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * constant to state that the message has no intended recipient.
	 */
	public static final int NO_INTENDED_RECIPIENT = -1;
	/**
	 * the intended recipient. It is the identity of a reachable server.
	 */
	private final int intendedRecipient;

	/**
	 * constructs a message.
	 * 
	 * @param sender            the identity of the sender.
	 * @param intendedRecipient the identity of the intended recipient, which is a
	 *                          reachable server.
	 * @param path              the path of servers.
	 */
	public ServerToServerMsgContent(final int sender, final int intendedRecipient, final List<Integer> path) {
		super(sender, path);
		this.intendedRecipient = intendedRecipient;
		assert invariantMsgContent();
	}

	/**
	 * constructs a message with no intended recipient (irrelevant intended
	 * recipient).
	 * 
	 * @param sender the identity of the sender.
	 * @param path   the path of servers.
	 */
	public ServerToServerMsgContent(final int sender, final List<Integer> path) {
		this(sender, NO_INTENDED_RECIPIENT, path);
	}

	/**
	 * constructs a message with an empty path, but with an intended recipient (this
	 * is a point-to-point message between servers).
	 * 
	 * @param sender            the identity of the sender.
	 * @param intendedRecipient the identity of the intended recipient.
	 */
	public ServerToServerMsgContent(final int sender, final int intendedRecipient) {
		this(sender, intendedRecipient, new ArrayList<>());
	}

	/**
	 * constructs a message with an empty path.
	 * 
	 * @param sender the identity of the sender.
	 */
	public ServerToServerMsgContent(final int sender) {
		this(sender, new ArrayList<>());
	}

	/**
	 * gets the intended recipient, which is a reachable server.
	 * 
	 * @return the identity of the intended recipient.
	 */
	public int getIntendedRecipient() {
		return intendedRecipient;
	}
}
