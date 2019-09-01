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

import java.nio.channels.SelectionKey;
import java.util.Objects;

/**
 * This class defines the routing information to a remote server.
 * 
 * @author Denis Conan
 * 
 */
public class RoutingInformation {
	/**
	 * the identity of the remote server.
	 */
	private int indentityOfRemoteServer;
	/**
	 * the length of the path to the remote server.
	 */
	private int lengthOfThePath;
	/**
	 * the identity of the neighbouring server in the path to the remote server,
	 * that is the identity of the first server of the path to the remote server.
	 */
	private int identityNeighbouringServer;
	/**
	 * the selection key to the neighbouring server.
	 */
	private SelectionKey selectionKeyOfNeighbouringServer;

	/**
	 * constructs an object.
	 * 
	 * @param indentityOfRemoteServer          the identity of the remote server.
	 * @param lengthOfThePath                  the length of the path that has been
	 *                                         found.
	 * @param identityNeighbouringServer       the identity of the server for the
	 *                                         first hop of the path.
	 * @param selectionKeyOfNeighbouringServer the selection key of the first hop.
	 */
	public RoutingInformation(final int indentityOfRemoteServer, final int lengthOfThePath,
			final int identityNeighbouringServer, final SelectionKey selectionKeyOfNeighbouringServer) {
		if (indentityOfRemoteServer < 0) {
			throw new IllegalArgumentException("the identity of the remote server cannot be negative");
		}
		if (lengthOfThePath < 0) {
			throw new IllegalArgumentException("the length of the path to the remote server cannot be negative");
		}
		if (identityNeighbouringServer < 0) {
			throw new IllegalArgumentException("the identity of the neighbouring server cannot be negative");
		}
		Objects.requireNonNull(selectionKeyOfNeighbouringServer,
				"the selection key to the neighbouring server cannot be null");
		this.indentityOfRemoteServer = indentityOfRemoteServer;
		this.lengthOfThePath = lengthOfThePath;
		this.identityNeighbouringServer = identityNeighbouringServer;
		this.selectionKeyOfNeighbouringServer = selectionKeyOfNeighbouringServer;
	}

	/**
	 * gets the identity of the remote server.
	 * 
	 * @return the identity.
	 */
	public int getIndentityOfRemoteServer() {
		return indentityOfRemoteServer;
	}

	/**
	 * gets the length of the path to the remote server.
	 * 
	 * @return the length of the path.
	 */
	public int getLengthOfThePath() {
		return lengthOfThePath;
	}

	/**
	 * the identity of the first server of the path to the remote server.
	 * 
	 * @return the identity of the neighbouring server.
	 */
	public int getIdentityNeighbouringServer() {
		return identityNeighbouringServer;
	}

	/**
	 * gets the selection key to the neighbouring server.
	 * 
	 * @return the selection key.
	 */
	public SelectionKey getSelectionKeyOfNeighbouringServer() {
		return selectionKeyOfNeighbouringServer;
	}

	@Override
	public int hashCode() {
		return Objects.hash(indentityOfRemoteServer);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof RoutingInformation)) {
			return false;
		}
		RoutingInformation other = (RoutingInformation) obj;
		return indentityOfRemoteServer == other.indentityOfRemoteServer;
	}

	@Override
	public String toString() {
		return "RoutingInformation [remote=" + indentityOfRemoteServer + ", lengthOfThePath=" + lengthOfThePath
				+ ", neighbour=" + identityNeighbouringServer + "]";
	}
}
